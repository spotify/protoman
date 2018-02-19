package com.spotify.protoman.registry;

import com.spotify.protoman.validation.SchemaValidator;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import org.postgresql.ds.PGConnectionPoolDataSource;

public class Main {

  private static final int GRPC_PORT = 8080;

  public static void main(final String...args) throws IOException {
    final PGConnectionPoolDataSource dataSource = new PGConnectionPoolDataSource();
    dataSource.setServerName("35.187.80.11");
    dataSource.setDatabaseName("postgres");
    dataSource.setPortNumber(5432);
    dataSource.setUser("postgres");
    dataSource.setPassword("aiF1ahk7meech8ie");


    final SchemaStorage schemaStorage = SqlSchemaStorage.create(dataSource);

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
