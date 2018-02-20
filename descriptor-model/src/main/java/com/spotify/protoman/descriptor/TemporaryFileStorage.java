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
