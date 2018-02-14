package com.spotify.protoman;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class Service {

  private static final int GRPC_PORT = 8080;

  public static void main(final String...args) throws IOException {
    final SchemaRegistryService service = SchemaRegistryService.create();
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
