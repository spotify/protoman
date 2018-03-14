package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
import java.util.HashSet;
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

public class SchemaRegistry implements SchemaPublisher, SchemaGetter {

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
      // and update dependencies for packages in the changed proto files
      updatedFiles(schemaFiles.stream(), currentDs, candidateDs).forEach(file -> {
        final String pkgName = candidateDs.findFileByPath(file.path()).get().fullName();
        final Set<Path> dependencies = resolvePackageDependencies(pkgName, candidateDs);
        tx.storeFile(file);
        tx.storePackageDependencies(pkgName, dependencies);
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

  private BuildDescriptorsResult buildDescriptorSets(final SchemaStorage.Transaction tx,
                                                     final ImmutableList<SchemaFile> schemaFiles)
      throws DescriptorBuilderException {
    // Paths of all updated files
    final ImmutableSet<Path> updatedPaths = schemaFiles.stream()
        .map(SchemaFile::path)
        .collect(toImmutableSet());

    try (final DescriptorBuilder descriptorBuilder =
             descriptorBuilderFactory.newDescriptorBuilder()) {
      // Seed descriptor builder with all files from registry
      // Builder DescriptorSet for what is currently in the registry for the files being updated
      final long snapshotVersion = tx.getLatestSnapshotVersion();
      final ImmutableMap<Path, SchemaFile> currentSchemata =
          tx.fetchAllFiles(snapshotVersion).collect(toImmutableMap(SchemaFile::path, Function.identity()));

      for (SchemaFile file : currentSchemata.values()) {
        descriptorBuilder.setProtoFile(file.path(), file.content());
      }

      // NOTE(staffan): As it is right now, we need compile ALL descriptors to catch breaking
      // changes.
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
  private static @Nullable DescriptorSet createFilteredDescriptorSet(
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
      final long latestSnapshotVersion = tx.getLatestSnapshotVersion();
      // Have to materialize inside the tx :/
      schemaFiles = tx.fetchFilesForPackage(latestSnapshotVersion, protoPackages)
          .collect(Collectors.toList());
    }
    return schemaFiles.stream();
  }

  @AutoValue
  abstract static class BuildDescriptorsResult {

    @Nullable abstract DescriptorSet current();

    @Nullable abstract DescriptorSet candidate();

    @Nullable abstract String currentCompilationError();

    @Nullable abstract String candidateCompilationError();

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

  private static Set<Path> resolvePackageDependencies(final String pkgName, final DescriptorSet descriptorSet) {
    // Build a dependency map
    final ImmutableMap<Path, FileDescriptor> fileDescriptorMap =
        descriptorSet.fileDescriptors().stream()
            .collect(toImmutableMap(FileDescriptor::filePath, Function.identity()));

    // Resolve dependencies
    final Set<Path> paths = new HashSet<>();
    final Queue<Path> q = new ArrayDeque<>();
    // Get all the files for the requested package
    final ImmutableList<Path> pathsForRequestedPackages = descriptorSet.fileDescriptors()
        .stream()
        .filter(fileDescriptor -> pkgName.equals(fileDescriptor.protoPackage()))
        .map(FileDescriptor::filePath)
        .collect(toImmutableList());
    q.addAll(pathsForRequestedPackages);
    while (!q.isEmpty()) {
      final Path path = q.poll();
      paths.add(path);
      fileDescriptorMap.get(path).dependencies().stream()
          .map(FileDescriptor::filePath)
          .filter(p -> !paths.contains(p))
          .forEach(q::add);
    }
    return paths;
  }
}
