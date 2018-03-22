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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.protobuf.DescriptorProtos;
import com.spotify.protoman.descriptor.DescriptorBuilder;
import com.spotify.protoman.descriptor.DescriptorBuilderException;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.registry.storage.SchemaStorage;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaRegistry implements SchemaPublisher, SchemaGetter {

  private static final Logger logger = LoggerFactory.getLogger(SchemaRegistry.class);

  private final SchemaStorage schemaStorage;
  private final SchemaValidator schemaValidator;
  private final SchemaVersioner schemaVersioner;
  private final DescriptorBuilder.Factory descriptorBuilderFactory;

  private SchemaRegistry(final SchemaStorage schemaStorage,
                         final SchemaValidator schemaValidator,
                         final SchemaVersioner schemaVersioner,
                         final DescriptorBuilder.Factory descriptorBuilderFactory) {
    this.schemaStorage = schemaStorage;
    this.schemaValidator = schemaValidator;
    this.schemaVersioner = schemaVersioner;
    this.descriptorBuilderFactory = descriptorBuilderFactory;
  }

  public static SchemaRegistry create(final SchemaStorage schemaStorage,
                                      final SchemaValidator schemaValidator,
                                      final SchemaVersioner schemaVersioner,
                                      final DescriptorBuilder.Factory descriptorBuilderFactory) {
    return new SchemaRegistry(
        schemaStorage, schemaValidator, schemaVersioner, descriptorBuilderFactory
    );
  }

  @Override
  public PublishResult publishSchemata(final ImmutableList<SchemaFile> schemaFiles) {
    try (final SchemaStorage.Transaction tx = schemaStorage.open()) {
      final BuildDescriptorsResult buildDescriptorsResult = buildDescriptorSets(tx, schemaFiles);

      if (buildDescriptorsResult.currentCompilationError() != null) {
        // Compilation of what's currently in the registry failed. This should not happen!
        throw new RuntimeException("Failed to build descriptor for current schemata");
      }

      if (buildDescriptorsResult.candidateCompilationError() != null) {
        return PublishResult.error(buildDescriptorsResult.candidateCompilationError());
      }

      final DescriptorSet currentDs = buildDescriptorsResult.current();
      final DescriptorSet candidateDs = buildDescriptorsResult.candidate();

      // Validate changes
      final ImmutableList<ValidationViolation> violations =
          schemaValidator.validate(currentDs, candidateDs);

      // TODO(staffan): Don't treat all violations as fatal
      if (!violations.isEmpty()) {
        return PublishResult.error("Validation failed", violations);
      }

      // Store files, but only for protos that changed
      updatedFiles(schemaFiles.stream(), currentDs, candidateDs)
          .forEach(file -> {
            final ImmutableSet<Path> dependencies =
                candidateDs.findFileByPath(file.path()).get().dependencies().stream()
                    .map(FileDescriptor::filePath)
                    .collect(toImmutableSet());
            logger.debug("proto: {}, deps: {}", file.path(), dependencies);
            tx.storeFile(file);
            tx.storeProtoDependencies(file.path(), dependencies);
          });

      // Update package versions (only for packages that have changed)
      final ImmutableMap<String, SchemaVersionPair> publishedPackages =
          updatePackageVersions(tx, currentDs, candidateDs);

      tx.commit();

      return PublishResult.create(violations, publishedPackages);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ImmutableSet<Path> resolveDependencies(final SchemaStorage.Transaction tx,
                                                 final long snapshotVersion,
                                                 final ImmutableSet<Path> paths) {

    final Set<Path> resultPaths = Sets.newHashSet();
    final Set<Path> resolvedPaths = Sets.newHashSet();
    final Queue<Path> q = new ArrayDeque<>();
    q.addAll(paths);

    while (!q.isEmpty()) {
      final Path path = q.poll();
      if (resolvedPaths.add(path)) {
        resultPaths.add(path);
        final ImmutableSet<Path> deps = tx.getDependencies(snapshotVersion, path)
            .collect(toImmutableSet());
        q.addAll(deps);
        logger.debug("deps path={} deps={}", path, deps);
      }
    }
    return ImmutableSet.copyOf(resultPaths);
  }

  private BuildDescriptorsResult buildDescriptorSets(final SchemaStorage.Transaction tx,
                                                     final ImmutableList<SchemaFile> schemaFiles)
      throws DescriptorBuilderException {

    final long snapshotVersion = tx.getLatestSnapshotVersion();

    // Paths of all updated files
    final ImmutableSet<Path> updatedPaths = schemaFiles.stream()
        .map(SchemaFile::path)
        .collect(toImmutableSet());

    final ImmutableSet<Path> updatedAndDependencies = resolveDependencies(tx,
        snapshotVersion, updatedPaths);

    try (final DescriptorBuilder descriptorBuilder =
             descriptorBuilderFactory.newDescriptorBuilder()) {
      // Seed descriptor builder with previous versions of all files and their dependencies

      final ImmutableMap<Path, SchemaFile> currentSchemata =
          updatedAndDependencies.stream()
              .map(path -> tx.schemaFile(snapshotVersion, path))
              .collect(toImmutableMap(SchemaFile::path, Function.identity()));

      for (SchemaFile file : currentSchemata.values()) {
        descriptorBuilder.setProtoFile(file.path(), file.content());
      }

      // NOTE(staffan): As it is right now, we need compile ALL descriptors to catch breaking
      // changes.
      // NOTE(fredrikd): Not compiling all any more, but keeping this message until
      // tests are added
      //
      // Consider the case where the following files exists in the repository:
      //
      // herp/derp.proto:
      //   package herp;
      //   message Derp {}
      // foo/bar.proto
      //   package foo;
      //   import "herp/derp.proto"
      //   message Bar {
      //     Derp derp = 1;
      //   }
      //
      // And a publish request that changes "herp/derp.proto" to:
      //   package herp {}
      //   message Herpaderp {} // Derp was removed/renamed!
      //
      //
      // That change will break "foo/bar.proto" -- BUT protoc will succeeded if we only run it
      // with foo/bar.proto as the input.
      //
      // So, we either need to either:
      // - Run protoc will ALL files as input, or
      // - Run protoc will ALL files that DEPEND ON a file being changed as input, or
      // - Track dependencies are ensured that types in use are not removed
      final DescriptorBuilder.Result currentResult = descriptorBuilder.buildDescriptor(
          currentSchemata.keySet().stream()
      );

      @Nullable final DescriptorSet currentDs = createFilteredDescriptorSet(
          currentResult.fileDescriptorSet(),
          updatedPaths
      );

      // Build DescriptorSet for the updated files
      for (SchemaFile schemaFile : schemaFiles) {
        descriptorBuilder.setProtoFile(schemaFile.path(), schemaFile.content());
      }

      final DescriptorBuilder.Result candidateResult = descriptorBuilder.buildDescriptor(
          Stream.concat(
              currentSchemata.keySet().stream(),
              schemaFiles.stream().map(SchemaFile::path)
          ).collect(toImmutableSet())
              .stream()
      );
      @Nullable final DescriptorSet candidateDs = createFilteredDescriptorSet(
          candidateResult.fileDescriptorSet(),
          updatedPaths
      );

      return BuildDescriptorsResult.create(
          currentDs, candidateDs,
          currentResult.compilationError(), candidateResult.compilationError()
      );
    }
  }

  /**
   * Given a {@link com.google.protobuf.DescriptorProtos.FileDescriptorSet} return a DescriptorSet
   * for any files matching the supplied paths.
   *
   * E.g. if given file descriptor set contains descriptors for a.proto, b.proto but {@code
   * includedPaths} only contains a.proto then the resulting {@link DescriptorSet} will only
   * contain a.proto.
   */
  private static @Nullable
  DescriptorSet createFilteredDescriptorSet(
      @Nullable final DescriptorProtos.FileDescriptorSet fileDescriptorSet,
      final ImmutableSet<Path> includedPaths) {
    return fileDescriptorSet != null
           ? DescriptorSet.create(fileDescriptorSet, includedPaths::contains)
           : null;
  }

  private ImmutableMap<String, SchemaVersionPair> updatePackageVersions(
      final SchemaStorage.Transaction tx,
      final DescriptorSet currentDs,
      final DescriptorSet candidateDs) {
    // Which packages are touched by the updated files?
    final ImmutableSet<String> touchedPackages = candidateDs.fileDescriptors().stream()
        .map(FileDescriptor::protoPackage)
        .collect(toImmutableSet());

    final Map<String, SchemaVersionPair> publishedPackages = new HashMap<>();

    // Update versions
    long snapshotVersion = tx.getLatestSnapshotVersion();
    touchedPackages.forEach(protoPackage -> {
      final Optional<SchemaVersion> currentVersion = tx.getPackageVersion(
          snapshotVersion,
          protoPackage
      );
      final SchemaVersion candidateVersion = schemaVersioner.determineVersion(
          protoPackage,
          currentVersion.orElse(null),
          currentDs,
          candidateDs
      );

      publishedPackages.put(
          protoPackage,
          SchemaVersionPair.create(candidateVersion, currentVersion.orElse(null))
      );

      if (!Objects.equals(currentVersion.orElse(null), candidateVersion)) {
        tx.storePackageVersion(protoPackage, candidateVersion);
      }
    });

    return ImmutableMap.copyOf(publishedPackages);
  }

  /**
   * Filters a stream of {@link SchemaFile}s returning only those that have been changed (in any
   * way, including formatting and comment changes. The exemption to this is that trailing
   * newline changes will not be considered as changes).
   */
  private static Stream<SchemaFile> updatedFiles(final Stream<SchemaFile> schemaFiles,
                                                 final DescriptorSet current,
                                                 final DescriptorSet candidate) {
    return schemaFiles.filter(schemaFile -> {
      final Optional<FileDescriptor> currentFd = current.findFileByPath(schemaFile.path());
      if (!currentFd.isPresent()) {
        return true;
      }

      final Optional<FileDescriptor> candidateFd =
          candidate.findFileByPath(schemaFile.path());

      return !Objects.equals(
          currentFd.get().toProto(),
          candidateFd.map(FileDescriptor::toProto).orElse(null)
      );
    });
  }

  @Override
  public Stream<SchemaFile> getSchemataForPackages(final ImmutableList<String> protoPackages) {
    final List<SchemaFile> schemaFiles;
    try (final SchemaStorage.Transaction tx = schemaStorage.open()) {
      final long snapshotVersion = tx.getLatestSnapshotVersion();

      final ImmutableSet<Path> protoPaths = protoPackages.stream()
          .flatMap(protoPackage -> tx.protosForPackage(snapshotVersion, protoPackage))
          .collect(toImmutableSet());

      logger.debug("deps pkgs={}, initial={}", protoPackages, protoPaths);
      final ImmutableSet<Path> dependencies = resolveDependencies(tx, snapshotVersion, protoPaths);
      logger.debug("dependencies={}", dependencies);

      schemaFiles = dependencies.stream()
          .map(path -> tx.schemaFile(snapshotVersion, path))
          .collect(Collectors.toList());
    }
    return schemaFiles.stream();
  }

  @Override
  public Stream<String> getPackageNames() {
    final SchemaStorage.Transaction tx = schemaStorage.open();
    final long latestSnapshotVersion = tx.getLatestSnapshotVersion();
    return tx.allPackageVersions(latestSnapshotVersion).keySet().stream();
  }

  @AutoValue
  abstract static class BuildDescriptorsResult {

    @Nullable
    abstract DescriptorSet current();

    @Nullable
    abstract DescriptorSet candidate();

    @Nullable
    abstract String currentCompilationError();

    @Nullable
    abstract String candidateCompilationError();

    static BuildDescriptorsResult create(
        @Nullable final DescriptorSet current,
        @Nullable final DescriptorSet candidate,
        @Nullable final String currentCompilationError,
        @Nullable final String candidateCompilationError) {
      return new AutoValue_SchemaRegistry_BuildDescriptorsResult(
          current,
          candidate,
          currentCompilationError,
          candidateCompilationError
      );
    }
  }
}
