package com.spotify.protoman.registry.storage;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.storage.Storage;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.spotify.protoman.registry.SchemaFile;
import com.spotify.protoman.registry.SchemaVersion;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
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

    logger.debug("Starting transaction from snapshot={}", indexGeneration);

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
        ProtoIndex currentProtoIndex = protoIndex(snapshotVersion);
        return currentProtoIndex.getProtoLocations().entrySet().stream()
            .map(e -> schemaFile(Paths.get(e.getKey()), e.getValue()));
      }

      @Override
      public void storePackageVersion(final String protoPackage, final SchemaVersion version) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        protoIndex.updatePackageVersion(protoPackage, version);
      }

      @Override
      public void storeProtoDependencies(final Path path, final Set<Path> paths) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        protoIndex.updateProtoDependencies(path, paths);
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
      public Stream<Path> protosForPackage(final long snapshotVersion, final String pkgName) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        ProtoIndex currentProtoIndex = protoIndex(snapshotVersion);
        return currentProtoIndex.getProtoLocations().keySet().stream()
            .map(Paths::get)
            .filter(packageFilter(pkgName));
      }

      @Override
      public Stream<Path> getDependencies(final long snapshotVersion, final Path path) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        ProtoIndex currentProtoIndex = protoIndex(snapshotVersion);
        return currentProtoIndex.getProtoDependencies().get(path).stream();
      }

      @Override
      public SchemaFile schemaFile(final long snapshotVersion, final Path path) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        ProtoIndex currentProtoIndex = protoIndex(snapshotVersion);
        return SchemaFile.create(path, fileContents(currentProtoIndex, path));
      }

      @Override
      public ImmutableMap<String, SchemaVersion> allPackageVersions(final long snapshotVersion) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        return ImmutableMap.copyOf(protoIndex(snapshotVersion).getPackageVersions());
      }

      @Override
      public long commit() {
        Preconditions.checkState(state.compareAndSet(TxState.OPEN, TxState.COMMITTED));

        final long snapshotVersion = indexFile.replace(protoIndex.toByteArray());
        logger.info("Committed. snapshotVersion={}", snapshotVersion);
        if (logger.isDebugEnabled()) {
          logger.debug("index={}", protoIndex.toProtoString());
        }
        return snapshotVersion;
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

      private Predicate<Path> packageFilter(final String pkgName) {
        Objects.requireNonNull(pkgName);
        final Path pkgPath = Paths.get(pkgName.replaceAll("\\.", "/"));
        return path -> path.getParent().equals(pkgPath);
      }

      private String fileContents(final ProtoIndex protoIndex, final Path path) {
        Objects.requireNonNull(protoIndex);
        Objects.requireNonNull(path);
        final String location = protoIndex.getProtoLocations().get(path.toString());
        if (location == null) {
          throw new RuntimeException("Location not found: " + path);
        }
        final byte[] bytes = protoStorage.get(HashCode.fromString(location)).orElseThrow(
            () -> new IllegalStateException("Location found. Missing data: " + path));
        return new String(bytes, UTF_8);
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
