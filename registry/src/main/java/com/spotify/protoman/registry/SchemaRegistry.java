package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.spotify.protoman.descriptor.DescriptorBuilder;
import com.spotify.protoman.descriptor.DescriptorBuilderException;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.registry.storage.SchemaStorage;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class SchemaRegistry implements SchemaPublisher {

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
      updatedFiles(schemaFiles.stream(), currentDs, candidateDs).forEach(tx::storeFile);

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
          //schemaFiles.stream()
          //    .map(SchemaFile::path)
          //    .filter(path -> currentSchemata.keySet().contains(path))
      );
      @Nullable final DescriptorSet currentDs =
          currentResult.fileDescriptorSet() != null ?
          DescriptorSet.create(
              currentResult.fileDescriptorSet(),
              path -> currentSchemata.keySet().contains(path)
          ) : null;

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
          //schemaFiles.stream()
          //    .map(SchemaFile::path)
      );
      @Nullable final DescriptorSet candidateDs =
          candidateResult.fileDescriptorSet() != null ?
          DescriptorSet.create(
              candidateResult.fileDescriptorSet(),
              path -> schemaFiles.stream().anyMatch(sf -> Objects.equals(sf.path(), path))
          ) : null;

      return BuildDescriptorsResult.create(
          currentDs, candidateDs,
          currentResult.compilationError(), candidateResult.compilationError()
      );
    }
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
}
