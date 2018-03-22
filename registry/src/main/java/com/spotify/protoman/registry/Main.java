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

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.spotify.protoman.descriptor.ProtocDescriptorBuilder;
import com.spotify.protoman.registry.storage.GcsSchemaStorage;
import com.spotify.protoman.registry.storage.SchemaStorage;
import com.spotify.protoman.validation.DefaultSchemaValidator;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class Main {

  public static final String DEFAULT_BUCKET_NAME = "protoman";

  private static final int GRPC_PORT = 8080;
  private static final String BUCKET_NAME = firstNonNull(
      System.getenv("PROTOMAN_BUCKET"),
      DEFAULT_BUCKET_NAME
  );

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

}
