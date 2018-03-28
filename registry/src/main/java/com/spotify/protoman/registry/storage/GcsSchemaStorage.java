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
  public static final String PROTO_PATH = "protos";
  public static final String PROTO_FILE_SUFFIX = "proto";

  private final ContentAddressedBlobStorage protoStorage;
  private final String bucket;
  private final Storage storage;

  private enum TxState {OPEN, COMMITTED, CLOSED}

  private GcsSchemaStorage(final Storage storage, final String bucket) {
    this.bucket = Objects.requireNonNull(bucket);
    this.storage = Objects.requireNonNull(storage);

    protoStorage = CachingContentAddressedBlobStorage.create(
        GcsContentAddressedBlobStorage.create(
            storage,
            bucket,
            PROTO_PATH,
            PROTO_FILE_SUFFIX
        ));

    // Create an empty index file in the bucket if it is empty
    indexFile().createIfNotExists(
        ProtoIndex.empty().toByteArray()
    );

  }

  public static GcsSchemaStorage create(final Storage storage, final String bucket) {
    return new GcsSchemaStorage(storage, bucket);
  }

  @Override
  public ReadAndWriteTransaction open() {
    final GcsGenerationalFile indexFile = indexFile();
    final ProtoIndex protoIndex = ProtoIndex.parse(indexFile.load());
    return new RwTx(indexFile, protoIndex);
  }

  @Override
  public ReadOnlyTransaction open(final long snapshotVersion) {
    final GcsGenerationalFile indexFile = indexFile();
    final ProtoIndex protoIndex = ProtoIndex.parse(
        indexFile().contentForGeneration(snapshotVersion)
    );
    return new RoTx(indexFile, protoIndex);
  }

  @Override
  public long getLatestSnapshotVersion() {
    final GcsGenerationalFile indexFile = indexFile();
    indexFile.load();
    return indexFile.currentGeneration();
  }

  @Override
  public Stream<Long> getSnapshotVersions() {
    return indexFile().listGenerations();
  }

  private GcsGenerationalFile indexFile() {
    return GcsGenerationalFile.create(
        storage,
        bucket,
        INDEX_BLOB_NAME
    );
  }

  private class RoTx implements ReadOnlyTransaction {

    protected final AtomicReference<TxState> state;
    protected final GcsGenerationalFile indexFile;
    protected final ProtoIndex protoIndex;

    private RoTx(final GcsGenerationalFile indexFile,
                 final ProtoIndex protoIndex) {

      this.indexFile = indexFile;
      this.protoIndex = protoIndex;
      state = new AtomicReference<>(TxState.OPEN);
    }

    @Override
    public Stream<SchemaFile> fetchAllFiles() {
      Preconditions.checkState(state.get() == TxState.OPEN);
      return protoIndex.getProtoLocations().entrySet().stream()
          .map(e -> schemaFile(Paths.get(e.getKey()), e.getValue()));
    }

    @Override
    public Optional<SchemaVersion> getPackageVersion(final String protoPackage) {
      Preconditions.checkState(state.get() == TxState.OPEN);
      final SchemaVersion schemaVersion = protoIndex
          .getPackageVersions()
          .get(protoPackage);
      return Optional.ofNullable(schemaVersion);
    }

    @Override
    public Stream<Path> protosForPackage(final String pkgName) {
      Preconditions.checkState(state.get() == TxState.OPEN);
      return protoIndex.getProtoLocations().keySet().stream()
          .map(Paths::get)
          .filter(packageFilter(pkgName));
    }

    @Override
    public Stream<Path> getDependencies(final Path path) {
      Preconditions.checkState(state.get() == TxState.OPEN);
      return protoIndex.getProtoDependencies().get(path).stream();
    }

    @Override
    public SchemaFile schemaFile(final Path path) {
      Preconditions.checkState(state.get() == TxState.OPEN);
      return SchemaFile.create(path, fileContents(protoIndex, path));
    }

    @Override
    public ImmutableMap<String, SchemaVersion> allPackageVersions() {
      Preconditions.checkState(state.get() == TxState.OPEN);
      return ImmutableMap.copyOf(protoIndex.getPackageVersions());
    }

    @Override
    public void close() {
      Preconditions.checkState(state.getAndSet(TxState.CLOSED) != TxState.CLOSED);
      // nothing do to
    }

    protected Predicate<Path> packageFilter(final String pkgName) {
      Objects.requireNonNull(pkgName);
      final Path pkgPath = Paths.get(pkgName.replaceAll("\\.", "/"));
      return path -> path.getParent().equals(pkgPath);
    }

    protected String fileContents(final ProtoIndex protoIndex, final Path path) {
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

    protected SchemaFile schemaFile(final Path path, final String hash) {
      Objects.requireNonNull(path);
      Objects.requireNonNull(hash);
      return SchemaFile.create(path,
          new String(protoStorage.get(HashCode.fromString(hash))
              .orElseThrow(() -> new RuntimeException("Not found: " + hash)), Charsets.UTF_8));
    }
  }

  private class RwTx extends RoTx implements ReadAndWriteTransaction {

    private RwTx(final GcsGenerationalFile indexFile,
                 final ProtoIndex protoIndex) {

      super(indexFile, protoIndex);
    }

    @Override
    public void storeFile(final SchemaFile file) {
      Preconditions.checkState(state.get() == TxState.OPEN);
      final HashCode hash = protoStorage.put(file.content().getBytes(Charsets.UTF_8));
      protoIndex.updateProtoLocation(file.path().toString(), hash.toString());
      logger.info("Stored file. path={} content={}", file.path(), hash.toString());
    }

    @Override
    public Stream<SchemaFile> fetchAllFiles() {
      Preconditions.checkState(state.get() == TxState.OPEN);
      return protoIndex.getProtoLocations().entrySet().stream()
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
    public void deleteFile(final Path path) {
      Preconditions.checkState(state.get() == TxState.OPEN);
      if (!protoIndex.removeProtoLocation(path.toString())) {
        throw new RuntimeException("Not found: " + path);
      }
    }
  }
}
