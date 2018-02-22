package com.spotify.protoman.registry;

import java.util.stream.Stream;

public class SchemaStorageAdapter implements SchemaStorage {

  private SchemaStorage2 delegate;

  public SchemaStorageAdapter(SchemaStorage2 delegate) {
    this.delegate = delegate;
  }

  @Override
  public Transaction open() {
    final SchemaStorage2.Transaction tx = delegate.open();

    return new Transaction() {
      @Override
      public void storeFile(final SchemaFile file) {
        tx.storeFile(file);
      }

      @Override
      public Stream<SchemaFile> fetchAllFiles() {
        return tx.fetchAllFiles(tx.getLatestSnapshotVersion());
      }

      @Override
      public void storePackageVersion(final String protoPackage, final SchemaVersion version) {
        tx.storePackageVersion(protoPackage, version);
      }

      @Override
      public SchemaVersion getPackageVersion(final String protoPackage) {
        return tx.getPackageVersion(tx.getLatestSnapshotVersion(), protoPackage).orElse(null);
      }

      @Override
      public void commit() {
        tx.commit();
      }

      @Override
      public void close() {
        tx.close();
      }
    };
  }
}
