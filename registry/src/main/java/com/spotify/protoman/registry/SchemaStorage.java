package com.spotify.protoman.registry;

import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public interface SchemaStorage {

  Transaction open();

  interface Transaction extends AutoCloseable {

    void storeFile(SchemaFile file);

    Stream<SchemaFile> fetchAllFiles(long snapshotVersion);

    void storePackageVersion(String protoPackage, SchemaVersion version);

    Optional<SchemaVersion> getPackageVersion(long snapshotVersion, String protoPackage);

    // Returns a new snapshot version if there were mutations.
    // If there are no mutations it returns the current snapshot version?
    long commit();

    long getLatestSnapshotVersion();

    Stream<Long> getSnapshotVersions();

    void deleteFile(Path path);

    void close();
  }
}
