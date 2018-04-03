/*-
 * -\-\-
 * protoman-registry
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.protoman.registry;

import static com.google.common.base.MoreObjects.firstNonNull;

import com.google.auto.value.AutoValue;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.spotify.jersey.support.JerseyServerComponentFactory;
import com.spotify.protoman.descriptor.ProtocDescriptorBuilder;
import com.spotify.protoman.registry.http.CORSFilter;
import com.spotify.protoman.registry.http.ProtobufJsonCodec;
import com.spotify.protoman.registry.http.SchemaResource;
import com.spotify.protoman.registry.storage.GcsSchemaStorage;
import com.spotify.protoman.registry.storage.SchemaStorage;
import com.spotify.protoman.validation.DefaultSchemaValidator;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final int GRPC_PORT = 9090;
  private static final int HTTP_PORT = 8080;
  private static final String BUCKET_NAME = firstNonNull(
      System.getenv("PROTOMAN_BUCKET"), "protoman");

  public static void main(final String... args) throws IOException {
    final SchemaRegistry schemaRegistry = createSchemaRegistry();

    final SchemaRegistryService registryService = SchemaRegistryService.create(
        schemaRegistry,
        schemaRegistry
    );
    final SchemaProtodocService protodocService = SchemaProtodocService.create(
        schemaRegistry
    );

    final Server grpcServer = ServerBuilder
        .forPort(GRPC_PORT)
        .addService(registryService)
        .addService(protodocService)
        .intercept(new LoggingServerInterceptor())
        .build();

    grpcServer.start();

    // HTTP server
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    final ResourceConfig rc = resourceConfig(schemaRegistry);
    // static files
    final Path staticFilePath = staticFilesPathFromEnv();
    logger.info("Loading static files from " + staticFilePath);
    Files.list(staticFilePath).forEach(p -> logger.debug(p.toAbsolutePath().toString()));
    final StaticHttpHandler staticHttpHandler =
        new IndexFallbackStaticHttpHandler(staticFilePath.toAbsolutePath().toString());

    final ExtraHandler staticExtraHandler = ExtraHandler.create(staticHttpHandler, "/");
    URI uri = UriBuilder.fromUri("http://0.0.0.0/api").port(HTTP_PORT).build();
    startHttpServer(uri, rc, staticExtraHandler);

    while (!grpcServer.isTerminated()) {
      try {
        grpcServer.awaitTermination();
      } catch (InterruptedException e) {
      }
    }
  }

  private static SchemaRegistry createSchemaRegistry() {

    final Storage gcsStorage = StorageOptions.getDefaultInstance().getService();

    final SchemaStorage schemaStorage = GcsSchemaStorage.create(gcsStorage, BUCKET_NAME);

    return SchemaRegistry.create(
        schemaStorage,
        DefaultSchemaValidator.withDefaultRules(),
        SemverSchemaVersioner.create(),
        ProtocDescriptorBuilder.factoryBuilder().build()
    );
  }


  static class IndexFallbackStaticHttpHandler extends StaticHttpHandler {

    private IndexFallbackStaticHttpHandler(final String... docRoots) {
      super(docRoots);
    }

    @Override
    protected void onMissingResource(final Request request, final Response response)
        throws Exception {
      if (request.getRequestURI().matches("^/[^/]*$")) {
        if (!handle("/index.html", request, response)) {
          super.onMissingResource(request, response);
        }
      } else {
        super.onMissingResource(request, response);
      }
    }
  }

  @AutoValue
  abstract static class ExtraHandler {

    abstract HttpHandler handler();

    abstract ImmutableSet<String> mappings();

    static ExtraHandler create(final HttpHandler handler, final String... mappings) {
      return create(handler, ImmutableSet.copyOf(mappings));
    }

    static ExtraHandler create(final HttpHandler handler, final ImmutableSet<String> mappings) {
      return new AutoValue_Main_ExtraHandler(handler, mappings);
    }
  }

  private static void startHttpServer(
      final URI uri, final ResourceConfig rc, final ExtraHandler... extraHandlers)
      throws IOException {
    HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(uri, rc, false);

    for (final ExtraHandler extraHandler : extraHandlers) {
      httpServer
          .getServerConfiguration()
          .addHttpHandler(extraHandler.handler(), extraHandler.mappings().toArray(new String[0]));
    }

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    httpServer.shutdown(10, TimeUnit.SECONDS).get();
                  } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                  }
                }));
    httpServer.start();
  }

  private static ResourceConfig resourceConfig(SchemaRegistry schemaRegistry) {
    return new ResourceConfig()
        .setApplicationName("protoman")
        .registerInstances(JerseyServerComponentFactory.defaultComponents())
        .register(ProtobufJsonCodec.class)
        .register(CORSFilter.class)
        .register(SchemaResource.create(schemaRegistry));
  }

  private static Path staticFilesPathFromEnv() {
    String webAppEnv = System.getenv("WEB_APP_PATH");
    if (Strings.isNullOrEmpty(webAppEnv)) {
      webAppEnv = "protodoc/build";
    }
    final Path webAppPath = Paths.get(webAppEnv);
    if (!Files.exists(webAppPath) || !Files.isDirectory(webAppPath)) {
      throw new IllegalStateException(
          "WEB_APP_PATH directory " + webAppPath.toAbsolutePath().toString() + "not found");
    }
    return webAppPath;
  }
}
