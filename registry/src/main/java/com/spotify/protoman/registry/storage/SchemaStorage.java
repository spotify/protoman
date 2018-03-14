package com.spotify.protoman.registry.storage;

import com.google.common.collect.ImmutableMap;
import com.spotify.protoman.registry.SchemaFile;
import com.spotify.protoman.registry.SchemaVersion;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface SchemaStorage {

  Transaction open();

  interface Transaction extends AutoCloseable {

    void storeFile(SchemaFile file);

    Stream<SchemaFile> fetchAllFiles(long snapshotVersion);

    void storePackageVersion(String protoPackage, SchemaVersion version);

    void storePackageDependencies(String protoPackage, Set<Path> paths);

    Optional<SchemaVersion> getPackageVersion(long snapshotVersion, String protoPackage);

    Stream<SchemaFile> fetchFilesForPackage(long snapshotVersion, List<String> protoPackages);

    ImmutableMap<String, SchemaVersion> allPackageVersions(long snapshotVersion);

    long commit();

    long getLatestSnapshotVersion();

    Stream<Long> getSnapshotVersions();

    void deleteFile(Path path);

    void close();
  }
}
