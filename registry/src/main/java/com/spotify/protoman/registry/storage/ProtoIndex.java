package com.spotify.protoman.registry.storage;


import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.spotify.protoman.Index;
import com.spotify.protoman.PackageDependency;
import com.spotify.protoman.Version;
import com.spotify.protoman.registry.SchemaVersion;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtoIndex {

  private static final Logger logger = LoggerFactory.getLogger(ProtoIndex.class);

  private static final byte[] EMPTY_INDEX = Index.newBuilder().build().toByteArray();

  private final Map<String, String> protoLocations;
  private final Map<String, SchemaVersion> packageVersions;
  private final Multimap<String, Path> packageDependencies;

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
    protoLocations = Maps.newHashMap(index.getProtoLocationsMap());

    packageVersions = index.getPackageVersionsMap().entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> toSchemaVersion(e.getValue())));

    packageDependencies = index.getPackageDependeciesList().stream()
        .collect(Multimaps.toMultimap(
            PackageDependency::getPackage,
            packageDependency -> Paths.get(packageDependency.getPath()),
            HashMultimap::create));
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

  public void updatePackageDependencies(final String pkg, final Set<Path> paths) {
    packageDependencies.replaceValues(pkg, paths);
  }

  public ImmutableSetMultimap<String, Path> getPackageDependencies() {
    return ImmutableSetMultimap.copyOf(packageDependencies);
  }

  public boolean removeProtoLocation(final String pkg) {
    Objects.requireNonNull(pkg);
    return protoLocations.remove(pkg) != null;
  }

  public Map<String, String> getProtoLocations() {
    return ImmutableMap.copyOf(protoLocations);
  }

  public Map<String, SchemaVersion> getPackageVersions() {
    return ImmutableMap.copyOf(packageVersions);
  }

  public byte[] toByteArray() {
    final Index.Builder builder = Index.newBuilder();
    builder.putAllProtoLocations(protoLocations);

    packageVersions.entrySet().forEach(e -> builder.putPackageVersions(e.getKey(),
        Version.newBuilder()
            .setMajor(e.getValue().major())
            .setMinor(e.getValue().minor())
            .setPatch(e.getValue().patch())
            .build()));

    packageDependencies.entries().forEach(e ->
        builder.addPackageDependecies(PackageDependency.newBuilder()
            .setPackage(e.getKey()).setPath(e.getValue().toString()).build()));

    if (logger.isDebugEnabled()) {
      logger.debug("index: {}", TextFormat.printToString(builder));
    }

    return builder.build().toByteArray();
  }

  private static SchemaVersion toSchemaVersion(final Version version) {
    return SchemaVersion.create(
        version.getMajor(),
        version.getMinor(),
        version.getPatch()
    );
  }

}
