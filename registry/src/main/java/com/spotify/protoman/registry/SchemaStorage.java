package com.spotify.protoman.registry;

import java.util.stream.Stream;

public interface SchemaStorage {

  Transaction open();

  interface Transaction extends AutoCloseable {

    void storeFile(SchemaFile file);

    Stream<SchemaFile> fetchAllFiles();

    void storePackageVersion(String protoPackage, SchemaVersion version);

    SchemaVersion getPackageVersion(String protoPackage);

    void commit();

    void close();
  }
}
