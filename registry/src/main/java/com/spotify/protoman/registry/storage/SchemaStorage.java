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

import com.google.common.collect.ImmutableMap;
import com.spotify.protoman.registry.SchemaFile;
import com.spotify.protoman.registry.SchemaVersion;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface SchemaStorage {

  ReadAndWriteTransaction open();

  ReadOnlyTransaction open(long snapshotVersion);

  long getLatestSnapshotVersion();

  Stream<Long> getSnapshotVersions();

  interface ReadOnlyTransaction extends AutoCloseable {

    Stream<SchemaFile> fetchAllFiles();

    Optional<SchemaVersion> getPackageVersion(String pkgName);

    Stream<Path> getDependencies(Path path);

    Stream<Path> protosForPackage(String pkgName);

    SchemaFile schemaFile(Path path);

    ImmutableMap<String, SchemaVersion> allPackageVersions();

    void close();
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
