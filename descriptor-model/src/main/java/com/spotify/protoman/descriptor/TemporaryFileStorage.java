/*-
 * -\-\-
 * protoman-descriptor-model
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

package com.spotify.protoman.descriptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

class TemporaryFileStorage implements AutoCloseable {

  private final Path root;

  private TemporaryFileStorage(final Path root) {
    this.root = root.toAbsolutePath();
  }

  static TemporaryFileStorage create(final String prefix) throws IOException {
    final Path tempDirectory = Files.createTempDirectory(prefix);
    return new TemporaryFileStorage(tempDirectory);
  }

  void storeFile(final String fileName, final byte[] contents) throws IOException {
    Path filePath = root.resolve(fileName);
    Files.createDirectories(filePath.getParent());
    Files.write(filePath, contents);
  }

  Path root() {
    return root;
  }

  @Override
  public void close() throws IOException {
    deleteRecursive(root);
  }

  private static void deleteRecursive(final Path path) throws IOException {
    Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }
}
