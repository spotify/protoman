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

    void storeProtoDependencies(Path path, Set<Path> paths);

    Optional<SchemaVersion> getPackageVersion(long snapshotVersion, String protoPackage);

    Stream<Path> getDependencies(long snapshotVersion, Path path);

    Stream<Path> protosForPackage(long snapshotVersion, String pkgName);

    SchemaFile schemaFile(long snapshotVersion, Path path);

    ImmutableMap<String, SchemaVersion> allPackageVersions(long snapshotVersion);

    long commit();

    long getLatestSnapshotVersion();

    Stream<Long> getSnapshotVersions();

    void deleteFile(Path path);

    void close();
  }
}

/*

TODO(fredrikd): refactor to something like this

public interface SchemaStorage {

  ReadAndWriteTransaction open();

  ReadOnlyTransaction open(long snapshotVersion);

  long getLatestSnapshotVersion();

  Stream<Long> getSnapshotVersions();


  interface ReadOnlyTransaction {

    Stream<SchemaFile> fetchAllFiles();

    Optional<SchemaVersion> getPackageVersion(String pkgName);

    Stream<Path> resolveDependencies(Path path);

    Stream<Path> protosForPackage(String pkgName);

    SchemaFile schemaFile(Path path);

    ImmutableMap<String, SchemaVersion> allPackageVersions();
  }


  interface ReadAndWriteTransaction extends ReadOnlyTransaction, AutoCloseable {

    void storeFile(SchemaFile file);

    void storePackageVersion(String pkgName, SchemaVersion version);

    void storeProtoDependencies(Path path, Set<Path> paths);

    void deleteFile(Path path);

    long commit();

    void close();
  }
}

 */