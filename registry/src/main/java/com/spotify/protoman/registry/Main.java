package com.spotify.protoman.registry;

import com.spotify.protoman.descriptor.ProtocDescriptorBuilder;
import com.spotify.protoman.validation.DefaultSchemaValidator;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.sql.SQLException;

public class Main {

  private static final int GRPC_PORT = 8080;

  private static final String BUCKET_NAME = "protoman";

  public static void main(final String... args) throws IOException, SQLException {
    final SchemaRegistry schemaRegistry = createSchemaRegistry();

    final SchemaRegistryService service = SchemaRegistryService.create(schemaRegistry);
    final Server grpcServer = ServerBuilder.forPort(GRPC_PORT).addService(service).build();

    grpcServer.start();

    while (!grpcServer.isTerminated()) {
      try {
        grpcServer.awaitTermination();
      } catch (InterruptedException e) {
      }
    }
  }

  private static SchemaRegistry createSchemaRegistry() {

    final SchemaStorage schemaStorage = new SchemaStorageAdapter(
        new GcsSchemaStorage(BUCKET_NAME)
    );

    return SchemaRegistry.create(
        schemaStorage,
        DefaultSchemaValidator.withDefaultRules(),
        SemverSchemaVersioner.create(),
        ProtocDescriptorBuilder.factoryBuilder().build()
    );
  }
}
