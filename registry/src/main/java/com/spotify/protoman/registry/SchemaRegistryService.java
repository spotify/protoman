/*-
 * -\-\-
 * protoman-registry
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.Error;
import com.spotify.protoman.FilePosition;
import com.spotify.protoman.GetSchemaRequest;
import com.spotify.protoman.GetSchemaResponse;
import com.spotify.protoman.ProtoFile;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaRegistryService extends SchemaRegistryGrpc.SchemaRegistryImplBase {

  private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryService.class);

  private final SchemaPublisher schemaPublisher;
  private final SchemaGetter schemaGetter;

  private SchemaRegistryService(final SchemaPublisher schemaPublisher,
                                final SchemaGetter schemaGetter) {
    this.schemaPublisher = schemaPublisher;
    this.schemaGetter = schemaGetter;
  }

  public static SchemaRegistryService create(final SchemaPublisher schemaPublisher,
                                             final SchemaGetter schemaGetter) {
    return new SchemaRegistryService(schemaPublisher, schemaGetter);
  }

  @Override
  public void publishSchema(final PublishSchemaRequest request,
                            final StreamObserver<PublishSchemaResponse> responseObserver) {
    try {
      final PublishSchemaResponse.Builder responseBuilder = PublishSchemaResponse.newBuilder();

      final ImmutableList<SchemaFile> schemaFiles = request.getProtoFileList().stream()
          .map(protoFile -> SchemaFile.create(
              Paths.get(protoFile.getPath()),
              protoFile.getContent())
          ).collect(toImmutableList());

      final SchemaPublisher.PublishResult result = schemaPublisher.publishSchemata(
          schemaFiles,
          request.getDryRun(),
          request.getForce()
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
      logger.error("publishSchema: {}", e.toString(), e);
      // TODO(staffan): Return errors in some sane way?
      // We encountered an unexpected error. E.g. we couldn't run protoc at all because we're out
      // of disk.
      responseObserver.onError(e);
    }
  }

  @Override
  public void getSchema(final GetSchemaRequest request,
                        final StreamObserver<GetSchemaResponse> responseObserver) {
    try {
      // TODO(staffan): Have some to handle expected errors. E.g. if the request package does not
      // exist
      final GetSchemaResponse.Builder responseBuilder = GetSchemaResponse.newBuilder();
      schemaGetter.getSchemataForPackages(request.getRequestList().stream()
          .map(GetSchemaRequest.RequestedPackage::getPackage)
          .collect(toImmutableList())
      ).map(SchemaRegistryService::schemaFileToProto)
          .forEach(responseBuilder::addProtoFile);
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (Exception e) {
      logger.error("getSchema: {}", e.toString(), e);
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

  private static ProtoFile schemaFileToProto(final SchemaFile schemaFile) {
    return ProtoFile.newBuilder()
        .setPath(schemaFile.path().toString())
        .setContent(schemaFile.content())
        .build();
  }
}
