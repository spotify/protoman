package com.spotify.protoman.registry.storage;

import com.google.cloud.storage.Storage;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimaps;
import com.google.common.hash.HashCode;
import com.spotify.protoman.registry.SchemaFile;
import com.spotify.protoman.registry.SchemaVersion;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcsSchemaStorage implements SchemaStorage {

  private static final Logger logger = LoggerFactory.getLogger(GcsSchemaStorage.class);

  private static final String INDEX_BLOB_NAME = "index.pb";

  private final ContentAddressedBlobStorage protoStorage;
  private final GcsGenerationalFile indexFile;

  private enum TxState {OPEN, COMMITTED, CLOSED}

  private GcsSchemaStorage(final Storage storage, final String bucket) {
    Objects.requireNonNull(bucket);
    Objects.requireNonNull(storage);
    protoStorage = CachingContentAddressedBlobStorage.create(
        GcsContentAddressedBlobStorage.create(
            storage,
            bucket,
            "protos",
            "proto"
        ));

    indexFile = GcsGenerationalFile.create(
        storage,
        bucket,
        INDEX_BLOB_NAME
    );

    indexFile.createIfNotExists(
        ProtoIndex.empty().toByteArray()
    );
  }

  public static GcsSchemaStorage create(final Storage storage, final String bucket) {
    return new GcsSchemaStorage(storage, bucket);
  }

  @Override
  public Transaction open() {
    final ProtoIndex protoIndex = ProtoIndex.parse(indexFile.load());
    long indexGeneration = indexFile.currentGeneration();

    logger.info("Starting transaction from snapshot={}", indexGeneration);

    return new Transaction() {
      final AtomicReference<TxState> state = new AtomicReference<>(TxState.OPEN);

      @Override
      public void storeFile(final SchemaFile file) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        final HashCode hash = protoStorage.put(file.content().getBytes(Charsets.UTF_8));
        protoIndex.updateProtoLocation(file.path().toString(), hash.toString());
        logger.info("Stored file. path={} content={}", file.path(), hash.toString());
      }

      @Override
      public Stream<SchemaFile> fetchAllFiles(final long snapshotVersion) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        ProtoIndex currentProtoIndex = protoIndex;

        if (snapshotVersion != indexGeneration) {
          currentProtoIndex = ProtoIndex.parse(indexFile.contentForGeneration(snapshotVersion));
        }
        return currentProtoIndex.getProtoLocations().entrySet().stream()
            .map(e -> SchemaFile.create(Paths.get(e.getKey()),
                new String(protoStorage.get(HashCode.fromString(e.getValue()))
                    .orElseThrow(() -> new RuntimeException("Not found")), Charsets.UTF_8)));
      }

      @Override
      public void storePackageVersion(final String protoPackage, final SchemaVersion version) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        protoIndex.updatePackageVersion(protoPackage, version);
      }

      @Override
      public void storePackageDependencies(final String protoPackage, final Set<Path> paths) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        protoIndex.updatePackageDependencies(protoPackage, paths);
      }

      @Override
      public Optional<SchemaVersion> getPackageVersion(final long snapshotVersion,
                                                       final String protoPackage) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        final SchemaVersion schemaVersion = protoIndex(snapshotVersion)
            .getPackageVersions().get(protoPackage);
        return Optional.ofNullable(schemaVersion);
      }

      @Override
      public Stream<SchemaFile> fetchFilesForPackage(final long snapshotVersion,
                                                     final List<String> protoPackages) {
        Preconditions.checkState(state.get() == TxState.OPEN);

        final ProtoIndex currentProtoIndex = protoIndex(snapshotVersion);
        final Map<String, String> protoLocations = currentProtoIndex.getProtoLocations();
        return Multimaps.filterKeys(currentProtoIndex.getPackageDependencies(),
            pkg -> protoPackages.contains(pkg)).values()
            .stream()
            .map(path -> schemaFile(path, protoLocations.get(path.toString())));
      }

      @Override
      public ImmutableMap<String, SchemaVersion> allPackageVersions(final long snapshotVersion) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        return ImmutableMap.copyOf(protoIndex(snapshotVersion).getPackageVersions());
      }

      @Override
      public long commit() {
        Preconditions.checkState(state.compareAndSet(TxState.OPEN, TxState.COMMITTED));
        return indexFile.replace(protoIndex.toByteArray());
      }

      @Override
      public long getLatestSnapshotVersion() {
        Preconditions.checkState(state.get() == TxState.OPEN);
        return indexFile.currentGeneration();
      }

      @Override
      public Stream<Long> getSnapshotVersions() {
        Preconditions.checkState(state.get() == TxState.OPEN);
        return indexFile.listGenerations();
      }

      @Override
      public void deleteFile(final Path path) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        if (!protoIndex.removeProtoLocation(path.toString())) {
          throw new RuntimeException("Not found: " + path);
        }
      }

      @Override
      public void close() {
        Preconditions.checkState(state.getAndSet(TxState.CLOSED) != TxState.CLOSED);
        // nothing do to
      }

      private ProtoIndex protoIndex(final long snapshotVersion) {
        if (snapshotVersion != indexGeneration) {
          return ProtoIndex.parse(indexFile.contentForGeneration(snapshotVersion));
        }
        return protoIndex;
      }

      private SchemaFile schemaFile(final Path path, final String hash) {
        Objects.requireNonNull(path);
        Objects.requireNonNull(hash);
        return SchemaFile.create(path,
            new String(protoStorage.get(HashCode.fromString(hash))
                .orElseThrow(() -> new RuntimeException("Not found: " + hash)), Charsets.UTF_8));
      }
    };
  }

}
