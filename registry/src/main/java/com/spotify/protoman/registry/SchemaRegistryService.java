package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.spotify.protoman.ProtoFile;
import com.spotify.protoman.PublishResult;
import com.spotify.protoman.PublishSchemaRequest;
import com.spotify.protoman.PublishSchemaResponse;
import com.spotify.protoman.SchemaRegistryGrpc;
import com.spotify.protoman.descriptor.DescriptorBuilder;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.descriptor.SourceCodeInfo;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import io.grpc.stub.StreamObserver;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;

public class SchemaRegistryService extends SchemaRegistryGrpc.SchemaRegistryImplBase {

  private final SchemaStorage schemaStorage;
  private final DescriptorBuilder.Factory descriptorBuilderFactory;
  private final SchemaValidator schemaValidator;
  private final SchemaVersioner schemaVersioner;

  private SchemaRegistryService(final SchemaStorage schemaStorage,
                                final DescriptorBuilder.Factory descriptorBuilderFactory,
                                final SchemaValidator schemaValidator,
                                final SchemaVersioner schemaVersioner) {
    this.schemaStorage = schemaStorage;
    this.descriptorBuilderFactory = descriptorBuilderFactory;
    this.schemaValidator = schemaValidator;
    this.schemaVersioner = schemaVersioner;
  }

  public static SchemaRegistryService create(
      final SchemaStorage schemaStorage,
      final DescriptorBuilder.Factory descriptorBuilderFactory,
      final SchemaValidator schemaValidator,
      final SchemaVersioner schemaVersioner) {
    return new SchemaRegistryService(
        schemaStorage, descriptorBuilderFactory, schemaValidator, schemaVersioner
    );
  }

  @Override
  public void publishSchema(final PublishSchemaRequest request,
                            final StreamObserver<PublishSchemaResponse> responseObserver) {
    // Paths of all protos being touched in this request
    final ImmutableList<Path> protoFilePaths = request.getProtoFileList().stream()
        .map(ProtoFile::getPath)
        .map(Paths::get)
        .collect(toImmutableList());

    try (final SchemaStorage.Transaction tx = schemaStorage.open();
         final DescriptorBuilder descriptorBuilder =
             descriptorBuilderFactory.newDescriptorBuilder()) {
      // Seed descriptor builder with all files from registry
      // Builder DescriptorSet for what is currently in the registry for the files being updated
      final ImmutableMap<Path, SchemaFile> currentSchemata =
          tx.fetchAllFiles().collect(toImmutableMap(SchemaFile::path, Function.identity()));
      currentSchemata.values().forEach(
          schemaFile -> descriptorBuilder.setProtoFile(schemaFile.path(), schemaFile.content())
      );
      final DescriptorSet currentDs = descriptorBuilder.buildDescriptor(
          protoFilePaths.stream()
              .filter(path -> currentSchemata.keySet().contains(path))
              .collect(toImmutableList())
      );

      // Build DescriptorSet for the updated files
      request.getProtoFileList()
          .forEach(protoFile -> descriptorBuilder.setProtoFile(
              Paths.get(protoFile.getPath()),
              protoFile.getContent().toStringUtf8())
          );
      final DescriptorSet candidateDs = descriptorBuilder.buildDescriptor(protoFilePaths);

      final PublishSchemaResponse.Builder responseBuilder = PublishSchemaResponse.newBuilder();

      final ImmutableList<ValidationViolation> violations =
          schemaValidator.validate(currentDs, candidateDs);
      violations.forEach(
          violation -> {
            final com.spotify.protoman.ValidationViolation.Builder violationBuilder =
                com.spotify.protoman.ValidationViolation.newBuilder()
                    .setDescription(violation.description());

            if (violation.candidate() != null) {
              final SourceCodeInfo sourceCodeInfo = violation.candidate().sourceCodeInfo().get();
              violationBuilder.setFilePath(sourceCodeInfo.filePath().toString());
              violationBuilder.setRow(sourceCodeInfo.start().line());
              violationBuilder.setColumn(sourceCodeInfo.start().column());
              violationBuilder.setReferencesOld(false);
            } else {
              assert violation.current() != null;
              final SourceCodeInfo sourceCodeInfo = violation.current().sourceCodeInfo().get();
              violationBuilder.setFilePath(sourceCodeInfo.filePath().toString());
              violationBuilder.setRow(sourceCodeInfo.start().line());
              violationBuilder.setColumn(sourceCodeInfo.start().column());
              violationBuilder.setReferencesOld(true);
            }

            responseBuilder.addViolation(violationBuilder.build());
          }
      );

      // Which packages are touched by the updated files?
      final ImmutableSet<String> touchedPackages = candidateDs.fileDescriptors().stream()
          .filter(fd -> protoFilePaths.contains(Paths.get(fd.name())))
          .map(FileDescriptor::protoPackage)
          .collect(toImmutableSet());

      // Store files
      request.getProtoFileList().forEach(protoFile -> {
        tx.storeFile(
            SchemaFile.create(
                Paths.get(protoFile.getPath()),
                protoFile.getContent().toStringUtf8()
            )
        );
      });

      // Update versions
      candidateDs.fileDescriptors().stream()
          .filter(fd -> protoFilePaths.contains(Paths.get(fd.name())))
          .map(FileDescriptor::protoPackage)
          .forEach(protoPackage -> {
            final Optional<SchemaVersion> currentVersion = tx.getPackageVersion(protoPackage);
            final SchemaVersion candidateVersion = schemaVersioner.determineVersion(
                protoPackage,
                currentVersion.orElse(null),
                currentDs,
                candidateDs
            );

            final PublishResult.Builder builder = PublishResult.newBuilder()
                .setPackage(protoPackage)
                .setVersion(
                    com.spotify.protoman.SchemaVersion.newBuilder()
                        .setMajor(candidateVersion.major())
                        .setMinor(candidateVersion.minor())
                        .setPatch(candidateVersion.patch())
                        .build()
                );

            currentVersion.ifPresent(v -> {
              builder.setPrevVersion(com.spotify.protoman.SchemaVersion.newBuilder()
                  .setMajor(v.major())
                  .setMinor(v.minor())
                  .setPatch(v.patch())
                  .build());
            });

            responseBuilder.addPublishResult(builder.build());

            tx.storePackageVersion(protoPackage, candidateVersion);
          });

      tx.commit();

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      responseObserver.onError(e);
    }
  }
}
