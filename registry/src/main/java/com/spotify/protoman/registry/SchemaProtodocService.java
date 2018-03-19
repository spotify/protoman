package com.spotify.protoman.registry;

import com.spotify.protoman.SchemaProtodocGrpc;
import com.spotify.protoman.GetSubPackagesRequest;
import com.spotify.protoman.GetSubPackagesResponse;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class SchemaProtodocService extends SchemaProtodocGrpc.SchemaProtodocImplBase {
  private static final Logger logger = LoggerFactory.getLogger(SchemaProtodocService.class);

  private final SchemaGetter schemaGetter;

  private SchemaProtodocService(final SchemaGetter schemaGetter) {
    this.schemaGetter = schemaGetter;
  }

  public static SchemaProtodocService create(final SchemaGetter schemaGetter) {
    return new SchemaProtodocService(schemaGetter);
  }

  @Override
  public void getSubPackages(final GetSubPackagesRequest request,
                             final StreamObserver<GetSubPackagesResponse> responseObserver) {
    try {
      final GetSubPackagesResponse.Builder responseBuilder = GetSubPackagesResponse.newBuilder();
      final String requestPackageName = request.getPackageName();

      final List<String> subPackageNames = schemaGetter
          .getPackageNames()
          .filter(name -> name.startsWith(requestPackageName))
          .map(name -> name.substring(requestPackageName.length()))
          .collect(Collectors.toList());

      responseBuilder.addAllSubPackageName(subPackageNames);
      responseObserver.onNext(responseBuilder.build());
      responseObserver.onCompleted();
    } catch (final Exception e) {
      logger.error("Failed to get subpackages: {}", e.toString(), e);
      responseObserver.onError(e);
    }
  }
}
