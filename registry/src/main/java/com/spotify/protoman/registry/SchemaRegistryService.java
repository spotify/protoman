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

public class SchemaRegistryService extends SchemaRegistryGrpc.SchemaRegistryImplBase {

  private final ProtoStore protoStore;
  private final DescriptorBuilder descriptorBuilder;
  private final SchemaValidator schemaValidator;

  private SchemaRegistryService(final ProtoStore protoStore,
                                final DescriptorBuilder descriptorBuilder,
                                final SchemaValidator schemaValidator) {
    this.protoStore = protoStore;
    this.descriptorBuilder = descriptorBuilder;
    this.schemaValidator = schemaValidator;
  }

  public static SchemaRegistryService create(final ProtoStore protoStore,
                                             final DescriptorBuilder descriptorBuilder,
                                             final SchemaValidator schemaValidator) {
    return new SchemaRegistryService(protoStore, descriptorBuilder, schemaValidator);
  }

  private void storeAllFiles(final TemporaryFileStorage fileStorage) {
    protoStore.getAll().forEach(protoFile -> {
      try {
        fileStorage.storeFile(protoFile.getPath(), protoFile.getContent().toByteArray());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
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
      final DescriptorProtos.FileDescriptorSet currentFds =
          descriptorBuilder.buildDescriptor(fileStorage.root(),
              protoFilePaths.stream()
                  .filter(relPath -> Files.exists(fileStorage.root().resolve(relPath)))
                  .collect(toImmutableList()));
      final DescriptorSet currentDs = DescriptorSet.create(currentFds, protoFilePaths::contains);

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

      protoStore.store(request.getProtoFileList().stream());

      responseObserver.onNext(PublishSchemaResponse.getDefaultInstance());
      responseObserver.onCompleted();
    } catch (IOException | InterruptedException e) {
      responseObserver.onError(e);
    }
  }
}
