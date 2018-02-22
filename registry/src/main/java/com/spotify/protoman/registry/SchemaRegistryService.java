package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.spotify.protoman.PublishResult;
import com.spotify.protoman.PublishSchemaRequest;
import com.spotify.protoman.PublishSchemaResponse;
import com.spotify.protoman.SchemaRegistryGrpc;
import com.spotify.protoman.descriptor.SourceCodeInfo;
import com.spotify.protoman.validation.ValidationViolation;
import io.grpc.stub.StreamObserver;
import java.nio.file.Paths;

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
                  protoFile.getContent().toStringUtf8())
              ).collect(toImmutableList())
      );

      result.violations().forEach(SchemaRegistryService::validationViolationToProto);

      result.publishedPackages().forEach(
          (protoPackage, versions) -> {
            final PublishResult.Builder builder = PublishResult.newBuilder()
                .setPackage(protoPackage)
                .setVersion(schemaVersionToProto(versions.version()));
            versions.prevVersion()
                .map(SchemaRegistryService::schemaVersionToProto)
                .ifPresent(builder::setPrevVersion);
            responseBuilder.addPublishResult(builder.build());
          }
      );

      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      // TODO(staffan): Return errors in some sane way
      responseObserver.onError(e);
    }
  }

  private static com.spotify.protoman.ValidationViolation validationViolationToProto(
      final ValidationViolation violation) {
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
