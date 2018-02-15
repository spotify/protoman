package com.spotify.protoman.registry;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.spotify.protoman.validation.SchemaValidator;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;

public class Main {

  private static final int GRPC_PORT = 8080;
  private static final String DEFAULT_GCS_BUCKET = "protoman-staffan-test";

  public static void main(final String...args) throws IOException {
    final Storage gcsClient = createGcsClient(GoogleCredentials.getApplicationDefault());
    final Bucket bucket = gcsClient.get(DEFAULT_GCS_BUCKET);
    final ProtoStore protoStore = GcsProtoStore.create(bucket);

    final DescriptorBuilder descriptorBuilder = ProtocDescriptorBuilder.create();

    final SchemaValidator schemaValidator = SchemaValidator.withDefaultRules();

    final SchemaRegistryService service = SchemaRegistryService.create(
        protoStore, descriptorBuilder, schemaValidator);
    final Server grpcServer = ServerBuilder.forPort(GRPC_PORT).addService(service).build();

    grpcServer.start();

    while (!grpcServer.isTerminated()) {
      try {
        grpcServer.awaitTermination();
      } catch (InterruptedException e) {
      }
    }
  }

  private static Storage createGcsClient(final GoogleCredentials credentials) {
    return StorageOptions.newBuilder()
        .setCredentials(credentials)
        .build()
        .getService();
  }
}
