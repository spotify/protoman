package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.spotify.protoman.descriptor.DescriptorBuilder;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

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
      final DescriptorSetPair descriptorSetPair = buildDescriptorSets(tx, schemaFiles);

      // Validate changes
      final ImmutableList<ValidationViolation> violations =
          schemaValidator.validate(descriptorSetPair.current(), descriptorSetPair.candidate());

      // TODO(staffan): Reject changes if there are severe violations

      // Store files
      schemaFiles.forEach(tx::storeFile);

      // Update package versions
      final ImmutableMap<String, SchemaVersionPair> publishedPackages =
          updatePackageVersions(tx, descriptorSetPair);

      tx.commit();

      return PublishResult.create(violations, publishedPackages);
    } catch (Exception e) {
      // TODO(staffan):
      throw new RuntimeException(e);
    }
  }

  private DescriptorSetPair buildDescriptorSets(final SchemaStorage.Transaction tx,
                                                final ImmutableList<SchemaFile> schemaFiles)
      throws Exception {
    final ImmutableSet<Path> schemaPaths =
        schemaFiles.stream().map(SchemaFile::path).collect(toImmutableSet());
    try (final DescriptorBuilder descriptorBuilder =
             descriptorBuilderFactory.newDescriptorBuilder()) {
      // Seed descriptor builder with all files from registry
      // Builder DescriptorSet for what is currently in the registry for the files being updated
      final ImmutableMap<Path, SchemaFile> currentSchemata =
          tx.fetchAllFiles().collect(toImmutableMap(SchemaFile::path, Function.identity()));
      currentSchemata.values().forEach(
          schemaFile -> descriptorBuilder.setProtoFile(schemaFile.path(), schemaFile.content())
      );
      final DescriptorSet currentDs = descriptorBuilder.buildDescriptor(
          schemaPaths.stream()
              .filter(path -> currentSchemata.keySet().contains(path))
      );

      // Build DescriptorSet for the updated files
      schemaFiles.forEach(schemaFile ->
          descriptorBuilder.setProtoFile(schemaFile.path(), schemaFile.content())
      );
      final DescriptorSet candidateDs = descriptorBuilder.buildDescriptor(schemaFiles.stream()
          .map(SchemaFile::path));

      return DescriptorSetPair.create(currentDs, candidateDs);
    }
  }

  private ImmutableMap<String, SchemaVersionPair> updatePackageVersions(
      final SchemaStorage.Transaction tx,
      final DescriptorSetPair descriptorSetPair) {
    final DescriptorSet currentDs = descriptorSetPair.current();
    final DescriptorSet candidateDs = descriptorSetPair.candidate();

    // Which packages are touched by the updated files?
    final ImmutableSet<String> touchedPackages = candidateDs.fileDescriptors().stream()
        .map(FileDescriptor::protoPackage)
        .collect(toImmutableSet());

    final Map<String, SchemaVersionPair> publishedPackages = new HashMap<>();

    // Update versions
    touchedPackages.forEach(protoPackage -> {
      final Optional<SchemaVersion> currentVersion = tx.getPackageVersion(protoPackage);
      final SchemaVersion candidateVersion = schemaVersioner.determineVersion(
          protoPackage,
          currentVersion.orElse(null),
          currentDs,
          candidateDs
      );

      publishedPackages.put(
          protoPackage,
          SchemaVersionPair.create(currentVersion.orElse(null), candidateVersion)
      );

      tx.storePackageVersion(protoPackage, candidateVersion);
    });

    return ImmutableMap.copyOf(publishedPackages);
  }

  @AutoValue
  static abstract class DescriptorSetPair {

    abstract DescriptorSet current();

    abstract DescriptorSet candidate();

    public static DescriptorSetPair create(
        final DescriptorSet current, final DescriptorSet candidate) {
      return new AutoValue_SchemaRegistry_DescriptorSetPair(current, candidate);
    }
  }
}
