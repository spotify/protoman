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
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

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

      if (buildDescriptorsResult.current().compilationError() != null) {
        // Compilation of what's currently in the registry failed. This should not happen!
        throw new RuntimeException("Failed to build descriptor for current schemata");
      }

      if (buildDescriptorsResult.candidate().compilationError() != null) {
        return PublishResult.error(buildDescriptorsResult.candidate().compilationError());
      }

      final DescriptorSet currentDs = buildDescriptorsResult.current().descriptorSet();
      final DescriptorSet candidateDs = buildDescriptorsResult.candidate().descriptorSet();

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
    try (final DescriptorBuilder descriptorBuilder = descriptorBuilderFactory.newDescriptorBuilder()) {
      // Seed descriptor builder with all files from registry
      // Builder DescriptorSet for what is currently in the registry for the files being updated
      final long snapshotVersion = tx.getLatestSnapshotVersion();
      final ImmutableMap<Path, SchemaFile> currentSchemata =
          tx.fetchAllFiles(snapshotVersion).collect(toImmutableMap(SchemaFile::path, Function.identity()));

      for (SchemaFile file : currentSchemata.values()) {
        descriptorBuilder.setProtoFile(file.path(), file.content());
      }

      final DescriptorBuilder.Result currentResult = descriptorBuilder.buildDescriptor(
          schemaFiles.stream()
              .map(SchemaFile::path)
              .filter(path -> currentSchemata.keySet().contains(path))
      );

      // Build DescriptorSet for the updated files
      for (SchemaFile schemaFile : schemaFiles) {
        descriptorBuilder.setProtoFile(schemaFile.path(), schemaFile.content());
      }

      final DescriptorBuilder.Result candidateResult = descriptorBuilder.buildDescriptor(
          schemaFiles.stream()
              .map(SchemaFile::path)
      );

      return BuildDescriptorsResult.create(currentResult, candidateResult);
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

    abstract DescriptorBuilder.Result current();

    abstract DescriptorBuilder.Result candidate();

    static BuildDescriptorsResult create(
        final DescriptorBuilder.Result current, final DescriptorBuilder.Result candidate) {
      return new AutoValue_SchemaRegistry_BuildDescriptorsResult(
          current,
          candidate
      );
    }
  }
}
