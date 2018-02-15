package com.spotify.protoman.registry;

import com.spotify.protoman.PublishSchemaRequest;
import com.spotify.protoman.PublishSchemaResponse;
import com.spotify.protoman.SchemaRegistryGrpc;
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
