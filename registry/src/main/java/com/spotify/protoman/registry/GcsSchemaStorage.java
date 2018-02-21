package com.spotify.protoman.registry;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;
import com.spotify.protoman.Index;
import com.spotify.protoman.PackageVersion;
import com.spotify.protoman.ProtoEntry;
import com.spotify.protoman.Version;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcsSchemaStorage implements SchemaStorage2 {

  private static final Logger logger = LoggerFactory.getLogger(GcsSchemaStorage.class);

  private static final String INDEX_BLOB_NAME = "index.pb";
  private static final String CONTENT_BLOB_NAME_TEMPLATE = "protos/%s/%s/%s.pb";
  private static final HashFunction CONTENT_HASH_FUNCTION = Hashing.sha256();

  private final Storage storage;
  private final String bucket;

  public GcsSchemaStorage(String bucket) {
    this.bucket = Objects.requireNonNull(bucket);
    storage = StorageOptions.getDefaultInstance().getService();
  }

  public void initBucket() {
    final Blob blob = storage.create(BlobInfo.newBuilder(bucket, INDEX_BLOB_NAME).build(),
        ProtoIndex.EMPTY_INDEX);

    logger.info("Init bucket: Writing an empty index file in bucket {}", bucket);
  }

  @Override
  public Transaction open() {
    final Blob indexBlob = storage.get(BlobId.of(bucket, INDEX_BLOB_NAME));
    if (indexBlob == null) {
      throw new IllegalStateException("No index file in bucket");
    }
    final long indexGeneration = indexBlob.getGeneration();
    final ProtoIndex protoIndex = ProtoIndex.parse(indexBlob.getContent());
    logger.info("Starting transaction from snapshot={}", indexGeneration);

    return new Transaction() {

      @Override
      public void storeFile(final SchemaFile file) {
        final String contentHash = hashContent(file.content());
        final String blobName = contentBlobName(contentHash);
        storage.create(BlobInfo.newBuilder(bucket, blobName).build(),
            file.content().getBytes(Charsets.UTF_8));
        protoIndex.updateMapping(file.path().toString(), contentHash);
        logger.info("Stored file. path={} content={}", file.path(), blobName);
      }

      @Override
      public Stream<SchemaFile> fetchAllFiles(final long snapshotVersion) {
        ProtoIndex currentProtoIndex = protoIndex;
        if (snapshotVersion != indexGeneration) {
          final Blob indexBlob = storage.get(BlobId.of(bucket, INDEX_BLOB_NAME, snapshotVersion));
          currentProtoIndex = ProtoIndex.parse(indexBlob.getContent());
        }
        return currentProtoIndex.protoLocations.entrySet().stream()
            .map(e -> SchemaFile.create(Paths.get(e.getKey()),
                new String(blobContent(contentBlobName(e.getValue())), Charsets.UTF_8)));
      }

      @Override
      public void storePackageVersion(final String protoPackage, final SchemaVersion version) {
        protoIndex.updatePackageVersion(protoPackage, version);
      }

      @Override
      public Optional<SchemaVersion> getPackageVersion(final long snapshotVersion,
                                                       final String protoPackage) {
        ProtoIndex currentProtoIndex = protoIndex;
        if (snapshotVersion != indexGeneration) {
          final Blob indexBlob = storage.get(BlobId.of(bucket, INDEX_BLOB_NAME, snapshotVersion));
          currentProtoIndex = ProtoIndex.parse(indexBlob.getContent());
        }
        return Optional.ofNullable(currentProtoIndex.packageVersions.get(protoPackage));
      }

      @Override
      public long commit() {
        try {
          final Blob blob = storage.create(
              BlobInfo.newBuilder(bucket, INDEX_BLOB_NAME, indexGeneration).build(),
              protoIndex.toByteArray(),
              Storage.BlobTargetOption.generationMatch());
          final Long newGeneration = blob.getGeneration();
          logger.info("Commited transaction. snapshotVersion={}", newGeneration);
          return newGeneration;
        } catch (StorageException ex) {
          throw new RuntimeException("Unable to commit transaction: " + ex);
        }
      }

      @Override
      public long getLatestSnapshotVersion() {
        return indexGeneration;
      }

      @Override
      public Stream<Long> getSnapshotVersions() {
        final Page<Blob> blobPage = storage.list(bucket,
            Storage.BlobListOption.prefix(INDEX_BLOB_NAME),
            Storage.BlobListOption.versions(true));

        final Iterable<Blob> blobIterable = () -> blobPage.iterateAll().iterator();

        return StreamSupport.stream(blobIterable.spliterator(), false)
            .filter(b -> b.getName().equals(INDEX_BLOB_NAME))
            .map(b -> b.getGeneration());
      }

      @Override
      public void deleteFile(final Path path) {
        if (protoIndex.protoLocations.remove(path) == null) {
          throw new RuntimeException("Not found: " + path);
        }
      }

      @Override
      public void close() {
      }

      private byte[] blobContent(final String name) {
        final Blob blob = storage.get(BlobId.of(bucket, name));
        if (blob == null) {
          throw new RuntimeException("Not found: " + name);
        }
        return blob.getContent();
      }

    };
  }

  private static String contentBlobName(final String contentHash) {
    Preconditions.checkState(contentHash.length() > 4);
    return String.format(CONTENT_BLOB_NAME_TEMPLATE,
        contentHash.substring(0, 2),
        contentHash.substring(2, 4),
        contentHash);
  }

  private static String hashContent(final String content) {
    final Hasher hasher = CONTENT_HASH_FUNCTION.newHasher();
    hasher.putString(content, Charsets.UTF_8);
    return hasher.hash().toString();
  }

  public static class ProtoIndex {

    public static final byte[] EMPTY_INDEX = Index.newBuilder().build().toByteArray();

    private Map<String, String> protoLocations = Maps.newHashMap();
    private Map<String, SchemaVersion> packageVersions = Maps.newHashMap();
    private Index index;

    private ProtoIndex(Index index) {
      this.index = index;
      protoLocations.putAll(index.getProtosList().stream()
          .collect(Collectors.toMap(
              p -> p.getPath(), p -> p.getBlobName())));

      packageVersions.putAll(index.getPackageVersionList().stream()
          .collect(Collectors.toMap(
              p -> p.getPackage(), p -> SchemaVersion.create(
                  p.getVersion().getMajor(),
                  p.getVersion().getMinor(),
                  p.getVersion().getPatch()))));
    }

    public void updateMapping(String src, String dest) {
      protoLocations.put(src, dest);
    }

    public void updatePackageVersion(String pkg, SchemaVersion version) {
      packageVersions.put(pkg, version);
    }

    public static ProtoIndex parse(byte[] bytes) {
      try {
        final Index index = Index.parseFrom(bytes);
        return new ProtoIndex(index);
      } catch (InvalidProtocolBufferException e) {
        throw new RuntimeException("Error parsing index file: " + e);
      }
    }

    public byte[] toByteArray() {
      final Index.Builder builder = Index.newBuilder(index);
      builder.clearProtos();
      protoLocations.entrySet().forEach(e -> builder.addProtos(
          ProtoEntry.newBuilder()
              .setPath(e.getKey())
              .setBlobName(e.getValue())
              .build()
      ));

      builder.clearPackageVersion();
      packageVersions.entrySet().forEach(e -> builder.addPackageVersion(
          PackageVersion.newBuilder()
              .setPackage(e.getKey())
              .setVersion(Version.newBuilder()
                  .setMajor(e.getValue().major())
                  .setMinor(e.getValue().minor())
                  .setPatch(e.getValue().patch())
                  .build())
      ));

      //if (logger.isDebugEnabled()) {
      logger.info("index: {}", TextFormat.printToString(builder));
      //}

      return builder.build().toByteArray();
    }
  }

  public static void main(String... args) {
    final GcsSchemaStorage storage = new GcsSchemaStorage("protoman");
    //storage.initBucket();

    try (final Transaction tx = storage.open()) {

      final long latestSnapshotVersion = tx.getLatestSnapshotVersion();
      final Stream<SchemaFile> allFiles = tx.fetchAllFiles(latestSnapshotVersion);
      logger.info("files: {}", allFiles.collect(Collectors.toList()));
      tx.storeFile(SchemaFile.create(
          Paths.get("/spotify/shameless/shameless.proto"), "a_proto"));
      tx.storeFile(SchemaFile.create(
          Paths.get("/spotify/shameless/shameless2.proto"), "another_przdfgsdfgoto2"));
      tx.storePackageVersion("spotify.shameless",
          SchemaVersion.create(1, 0, 0));
      tx.commit();
    }

    try (final Transaction tx = storage.open()) {
      tx.getSnapshotVersions().forEach(sv -> logger.info("snapshot={}, files={}", sv,
          tx.fetchAllFiles(sv).collect(Collectors.toList())));
    }
  }

}
