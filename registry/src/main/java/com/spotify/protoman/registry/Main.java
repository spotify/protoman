package com.spotify.protoman.registry;

import com.spotify.protoman.validation.SchemaValidator;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

  private static final int GRPC_PORT = 8080;

  private static final String DB_NAME = "postgres";
  private static final String DB_USER = "postgres";
  private static final String DB_PASSWORD = "aiF1ahk7meech8ie";
  private static final String CLOUD_SQL_INSTANCE_CONNECTION_NAME =
      "fabric-rnd:europe-west1:protoman-test";

  public static void main(final String...args) throws IOException, SQLException {
    final String jdbcUrl = String.format(
        "jdbc:postgresql://google/%s?socketFactory="
            + "&socketFactoryArg=%s",
        DB_NAME,
        CLOUD_SQL_INSTANCE_CONNECTION_NAME
    );

    final SchemaStorage schemaStorage;
    if ("true".equalsIgnoreCase(System.getenv("GCS"))) {
      schemaStorage = new SchemaStorageAdapter(new GcsSchemaStorage("protoman"));
    } else {
      schemaStorage = SqlSchemaStorage.create(
          () -> DriverManager.getConnection(jdbcUrl, DB_USER, DB_PASSWORD));
    }

    final DescriptorBuilder descriptorBuilder = ProtocDescriptorBuilder.create();

    final SchemaValidator schemaValidator = SchemaValidator.withDefaultRules();

    final SchemaRegistryService service = SchemaRegistryService.create(
        schemaStorage, descriptorBuilder, schemaValidator);
    final Server grpcServer = ServerBuilder.forPort(GRPC_PORT).addService(service).build();

    grpcServer.start();

    while (!grpcServer.isTerminated()) {
      try {
        grpcServer.awaitTermination();
      } catch (InterruptedException e) {
      }
    }
  }
}
