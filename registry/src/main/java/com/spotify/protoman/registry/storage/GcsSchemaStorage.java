package com.spotify.protoman.registry.storage;

import com.google.cloud.storage.Storage;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.spotify.protoman.registry.SchemaFile;
import com.spotify.protoman.registry.SchemaVersion;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcsSchemaStorage implements SchemaStorage {

  private static final Logger logger = LoggerFactory.getLogger(GcsSchemaStorage.class);

  private static final String INDEX_BLOB_NAME = "index.pb";

  private final ContentAddressedBlobStorage protoStorage;
  private final GcsGenerationalFile indexFile;

  private enum TxState { OPEN, COMMITED, CLOSED }

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
      public Optional<SchemaVersion> getPackageVersion(final long snapshotVersion,
                                                       final String protoPackage) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        ProtoIndex currentProtoIndex = protoIndex;
        if (snapshotVersion != indexGeneration) {
          currentProtoIndex = ProtoIndex.parse(indexFile.contentForGeneration(snapshotVersion));
        }
        return Optional.ofNullable(currentProtoIndex.getPackageVersions().get(protoPackage));
      }

      @Override
      public ImmutableMap<String, SchemaVersion> allPackageVersions(final long snapshotVersion) {
        Preconditions.checkState(state.get() == TxState.OPEN);
        ProtoIndex currentProtoIndex = protoIndex;
        if (snapshotVersion != indexGeneration) {
          currentProtoIndex = ProtoIndex.parse(indexFile.contentForGeneration(snapshotVersion));
        }
        return ImmutableMap.copyOf(currentProtoIndex.getPackageVersions());
      }

      @Override
      public long commit() {
        Preconditions.checkState(state.compareAndSet(TxState.OPEN, TxState.COMMITED));
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
    };
  }

}
