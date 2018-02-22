package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.spotify.protoman.Error;
import com.spotify.protoman.FilePosition;
import com.spotify.protoman.PublishSchemaRequest;
import com.spotify.protoman.PublishSchemaResponse;
import com.spotify.protoman.PublishedPackage;
import com.spotify.protoman.SchemaRegistryGrpc;
import com.spotify.protoman.descriptor.GenericDescriptor;
import com.spotify.protoman.descriptor.SourceCodeInfo;
import com.spotify.protoman.validation.ValidationViolation;
import io.grpc.stub.StreamObserver;
import java.nio.file.Paths;
import javax.annotation.Nullable;

public class SchemaRegistryService extends SchemaRegistryGrpc.SchemaRegistryImplBase {

  private final SchemaPublisher schemaPublisher;

  private SchemaRegistryService(final SchemaPublisher schemaPublisher) {
    this.schemaPublisher = schemaPublisher;
  }

  public static SchemaRegistryService create(final SchemaPublisher schemaPublisher) {
    return new SchemaRegistryService(schemaPublisher);
  }

  @Override
  public void publishSchema(final PublishSchemaRequest request,
                            final StreamObserver<PublishSchemaResponse> responseObserver) {
    try {
      final PublishSchemaResponse.Builder responseBuilder = PublishSchemaResponse.newBuilder();

      final SchemaPublisher.PublishResult result = schemaPublisher.publishSchemata(
          request.getProtoFileList().stream()
              .map(protoFile -> SchemaFile.create(
                  Paths.get(protoFile.getPath()),
                  protoFile.getContent())
              ).collect(toImmutableList())
      );

      result.error().ifPresent(
          error ->
              responseBuilder.setError(Error.newBuilder()
                  .setMessage(error)
                  .build()
              )
      );

      result.violations().stream()
          .map(SchemaRegistryService::validationViolationToProto)
          .forEach(responseBuilder::addViolation);

      result.publishedPackages().forEach(
          (protoPackage, versions) -> {
            final PublishedPackage.Builder builder = PublishedPackage.newBuilder()
                .setPackage(protoPackage)
                .setVersion(schemaVersionToProto(versions.version()));
            versions.prevVersion()
                .map(SchemaRegistryService::schemaVersionToProto)
                .ifPresent(builder::setPrevVersion);
            responseBuilder.addPublishedPackage(builder.build());
          }
      );

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      // TODO(staffan): Return errors in some sane way?
      // We encountered an unexpected error. E.g. we couldn't run protoc at all because we're out
      // of disk.
      responseObserver.onError(e);
    }
  }

  private static com.spotify.protoman.FilePosition sourceCodeInfoToFilePositionProto(
      final SourceCodeInfo sourceCodeInfo) {
    return FilePosition.newBuilder()
        .setPath(sourceCodeInfo.filePath().toString())
        .setLine(sourceCodeInfo.start().line())
        .setColumn(sourceCodeInfo.start().column())
        .build();
  }

  private static com.spotify.protoman.ValidationViolation validationViolationToProto(
      final ValidationViolation violation) {
    final com.spotify.protoman.ValidationViolation.Builder violationBuilder =
        com.spotify.protoman.ValidationViolation.newBuilder()
            .setDescription(violation.description());

    @Nullable final GenericDescriptor currentDescriptor = violation.current();
    if (currentDescriptor != null) {
      currentDescriptor.sourceCodeInfo().ifPresent(sourceCodeInfo ->
          violationBuilder.setCurrent(sourceCodeInfoToFilePositionProto(sourceCodeInfo))
      );
    }

    @Nullable final GenericDescriptor candidateDescriptor = violation.candidate();
    if (candidateDescriptor != null) {
      candidateDescriptor.sourceCodeInfo().ifPresent(sourceCodeInfo ->
          violationBuilder.setCandidate(sourceCodeInfoToFilePositionProto(sourceCodeInfo))
      );
    }

    return violationBuilder.build();
  }

  private static com.spotify.protoman.SchemaVersion schemaVersionToProto(final SchemaVersion v) {
    return com.spotify.protoman.SchemaVersion.newBuilder()
        .setMajor(v.major())
        .setMinor(v.minor())
        .setPatch(v.patch())
        .build();
  }
}
