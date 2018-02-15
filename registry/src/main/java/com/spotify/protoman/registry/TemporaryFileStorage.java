package com.spotify.protoman.registry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

class TemporaryFileStorage implements AutoCloseable {

  private final Path root;

  private TemporaryFileStorage(final Path root) {
    this.root = root;
  }

  static TemporaryFileStorage create() throws IOException {
    final Path tempDirectory = Files.createTempDirectory("schema-registry-");
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
