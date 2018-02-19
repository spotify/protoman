package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos;
import com.spotify.protoman.ProtoFile;
import com.spotify.protoman.PublishSchemaRequest;
import com.spotify.protoman.PublishSchemaResponse;
import com.spotify.protoman.SchemaRegistryGrpc;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;

public class SchemaRegistryService extends SchemaRegistryGrpc.SchemaRegistryImplBase {

  private final SchemaStorage schemaStorage;
  private final DescriptorBuilder descriptorBuilder;
  private final SchemaValidator schemaValidator;

  private SchemaRegistryService(final SchemaStorage schemaStorage,
                                final DescriptorBuilder descriptorBuilder,
                                final SchemaValidator schemaValidator) {
    this.schemaStorage = schemaStorage;
    this.descriptorBuilder = descriptorBuilder;
    this.schemaValidator = schemaValidator;
  }

  public static SchemaRegistryService create(final SchemaStorage schemaStorage,
                                             final DescriptorBuilder descriptorBuilder,
                                             final SchemaValidator schemaValidator) {
    return new SchemaRegistryService(schemaStorage, descriptorBuilder, schemaValidator);
  }

  private void storeAllFiles(final TemporaryFileStorage fileStorage) {
    try (final SchemaStorage.Transaction tx = schemaStorage.open()) {
      tx.fetchAllFiles()
          .forEach(file -> {
            try {
              fileStorage.storeFile(file.path().toString(), file.content().getBytes());
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }
  }

  @Override
  public void publishSchema(final PublishSchemaRequest request,
                            final StreamObserver<PublishSchemaResponse> responseObserver) {
    // Paths of all protos being touched in this request
    final ImmutableList<Path> protoFilePaths = request.getProtoFileList().stream()
        .map(ProtoFile::getPath)
        .map(Paths::get)
        .collect(toImmutableList());

    try (final TemporaryFileStorage fileStorage = TemporaryFileStorage.create()) {
      storeAllFiles(fileStorage);
      // Build FDS for all the files touched in this request IF they currently exist
      // in the registry
      final DescriptorSet currentDs;
      final ImmutableList<Path> currentPaths = protoFilePaths.stream()
          .filter(relPath -> Files.exists(fileStorage.root().resolve(relPath)))
          .collect(toImmutableList());
      if (currentPaths.isEmpty()) {
        currentDs = DescriptorSet.create(
            DescriptorProtos.FileDescriptorSet.getDefaultInstance(), __ -> true);
      } else {
        final DescriptorProtos.FileDescriptorSet currentFds =
            descriptorBuilder.buildDescriptor(fileStorage.root(), currentPaths);
        currentDs = DescriptorSet.create(currentFds, protoFilePaths::contains);
      }

      // Write any protos touched in this request
      for (final ProtoFile protoFile : request.getProtoFileList()) {
        fileStorage.storeFile(protoFile.getPath(), protoFile.getContent().toByteArray());
      }

      final DescriptorProtos.FileDescriptorSet candidateFds =
          descriptorBuilder.buildDescriptor(fileStorage.root(), protoFilePaths);
      final DescriptorSet candidateDs = DescriptorSet.create(
          candidateFds, protoFilePaths::contains);

      // TODO: Actually do something with violations (and remove println :))
      final ImmutableList<ValidationViolation> violations =
          schemaValidator.validate(currentDs, candidateDs);
      violations.forEach(violation -> {
        System.out.println(violation.description());
      });

      //schemaStorage.store(request.getProtoFileList().stream());
      try (final SchemaStorage.Transaction tx = schemaStorage.open()) {
        request.getProtoFileList().forEach(protoFile -> {
          tx.storeFile(
              SchemaFile.create(
                  Paths.get(protoFile.getPath()),
                  protoFile.getContent().toStringUtf8()
              )
          );
        });
        tx.commit();
      }

      responseObserver.onNext(PublishSchemaResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (IOException | InterruptedException e) {
      responseObserver.onError(e);
    }
  }
}
