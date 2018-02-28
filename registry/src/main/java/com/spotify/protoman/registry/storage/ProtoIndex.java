package com.spotify.protoman.registry.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.spotify.protoman.Index;
import com.spotify.protoman.PackageVersion;
import com.spotify.protoman.ProtoLocation;
import com.spotify.protoman.Version;
import com.spotify.protoman.registry.SchemaVersion;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtoIndex {

  private static final Logger logger = LoggerFactory.getLogger(ProtoIndex.class);

  private static final byte[] EMPTY_INDEX = Index.newBuilder().build().toByteArray();

  private final Map<String, String> protoLocations = Maps.newHashMap();
  private final Map<String, SchemaVersion> packageVersions = Maps.newHashMap();
  private final Index index;

  public static ProtoIndex empty() {
    return parse(EMPTY_INDEX);
  }

  public static ProtoIndex parse(final byte[] bytes) {
    try {
      final Index index = Index.parseFrom(bytes);
      return new ProtoIndex(index);
    } catch (InvalidProtocolBufferException e) {
      throw new RuntimeException("Error parsing index file: " + e);
    }
  }

  private ProtoIndex(final Index index) {
    this.index = index;
    protoLocations.putAll(index.getProtoLocationList().stream()
        .collect(Collectors.toMap(
            p -> p.getPath(), p -> p.getBlobName())));

    packageVersions.putAll(index.getPackageVersionList().stream()
        .collect(Collectors.toMap(
            p -> p.getPackage(), p -> SchemaVersion.create(
                p.getVersion().getMajor(),
                p.getVersion().getMinor(),
                p.getVersion().getPatch()))));
  }

  public void updateProtoLocation(final String src, final String dest) {
    Objects.requireNonNull(src);
    Objects.requireNonNull(dest);
    protoLocations.put(src, dest);
  }

  public void updatePackageVersion(final String pkg, final SchemaVersion version) {
    Objects.requireNonNull(pkg);
    Objects.requireNonNull(version);
    packageVersions.put(pkg, version);
  }

  public boolean removeProtoLocation(final String pkg) {
    return protoLocations.remove(pkg) != null;
  }

  public Map<String, String> getProtoLocations() {
    return ImmutableMap.copyOf(protoLocations);
  }

  public Map<String, SchemaVersion> getPackageVersions() {
    return ImmutableMap.copyOf(packageVersions);
  }

  public byte[] toByteArray() {
    final Index.Builder builder = Index.newBuilder(index);
    builder.clearProtoLocation();
    protoLocations.entrySet().forEach(e -> builder.addProtoLocation(
        ProtoLocation.newBuilder()
            .setPath(e.getKey())
            .setBlobName(e.getValue())
            .build()
    ));

    builder.clearPackageVersion();
    packageVersions.entrySet().forEach(e -> builder.addPackageVersion(
        PackageVersion.newBuilder()
            .setPackage(e.getKey())
            .setVersion(Version.newBuilder()
                .setMajor(e.getValue().major())
                .setMinor(e.getValue().minor())
                .setPatch(e.getValue().patch())
                .build())
    ));

    if (logger.isDebugEnabled()) {
      logger.debug("index: {}", TextFormat.printToString(builder));
    }

    return builder.build().toByteArray();
  }
}
