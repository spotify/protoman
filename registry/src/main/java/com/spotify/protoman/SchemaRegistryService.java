package com.spotify.protoman;

import io.grpc.stub.StreamObserver;

public class SchemaRegistryService extends SchemaRegistryGrpc.SchemaRegistryImplBase {

  private SchemaRegistryService() {
  }

  public static SchemaRegistryService create() {
    return new SchemaRegistryService();
  }

  @Override
  public void publishSchema(final PublishSchemaRequest request,
                            final StreamObserver<PublishSchemaResponse> responseObserver) {
    // TODO
  }
}
