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

package com.spotify.protoman.registry.storage;


import com.google.common.base.MoreObjects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.spotify.protoman.Index;
import com.spotify.protoman.ProtoDependency;
import com.spotify.protoman.Version;
import com.spotify.protoman.registry.SchemaVersion;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ProtoIndex {

  private static final byte[] EMPTY_INDEX = Index.newBuilder().build().toByteArray();

  private final Map<String, String> protoLocations; // TODO(fredrikd): <Path, HashCode> ?
  private final Map<String, SchemaVersion> packageVersions;
  private final Multimap<Path, Path> protoDependencies;

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

    protoDependencies = index.getProtoDependeciesList().stream()
        .collect(Multimaps.toMultimap(
            protoDependency -> Paths.get(protoDependency.getProtoPath()),
            protoDependency -> Paths.get(protoDependency.getDependencyPath()),
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

  public void updateProtoDependencies(final Path proto, final Set<Path> paths) {
    protoDependencies.replaceValues(proto, paths);
  }

  public ImmutableSetMultimap<Path, Path> getProtoDependencies() {
    return ImmutableSetMultimap.copyOf(protoDependencies);
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

  public String toProtoString() {
    return TextFormat.printToString(toProto());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("protoLocations", protoLocations)
        .add("packageVersions", packageVersions)
        .add("protoDependencies", protoDependencies)
        .toString();
  }

  private Index toProto() {
    final Index.Builder builder = Index.newBuilder();
    builder.putAllProtoLocations(protoLocations);

    packageVersions.entrySet().forEach(e -> builder.putPackageVersions(e.getKey(),
        Version.newBuilder()
            .setMajor(e.getValue().major())
            .setMinor(e.getValue().minor())
            .setPatch(e.getValue().patch())
            .build())
    );

    protoDependencies.entries().forEach(e ->
        builder.addProtoDependecies(ProtoDependency.newBuilder()
            .setProtoPath(e.getKey().toString())
            .setDependencyPath(e.getValue().toString())
            .build())
    );
    return builder.build();
  }

  public byte[] toByteArray() {
    return toProto().toByteArray();
  }

  private static SchemaVersion toSchemaVersion(final Version version) {
    return SchemaVersion.create(
        version.getMajor(),
        version.getMinor(),
        version.getPatch()
    );
  }

}
