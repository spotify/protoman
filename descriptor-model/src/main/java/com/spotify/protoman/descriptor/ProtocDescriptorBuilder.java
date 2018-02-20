package com.spotify.protoman.descriptor;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProtocDescriptorBuilder implements DescriptorBuilder {

  private TemporaryFileStorage fileStorage;

  private ProtocDescriptorBuilder(final TemporaryFileStorage fileStorage) {
    this.fileStorage = fileStorage;
  }

  public static ProtocDescriptorBuilder create() {
    try {
      return new ProtocDescriptorBuilder(TemporaryFileStorage.create("descriptor-builder-"));
    } catch (IOException e) {
      // TODO(staffan):
      throw new RuntimeException(e);
    }
  }

  public static Factory factory() {
    return ProtocDescriptorBuilder::create;
  }

  @Override
  public DescriptorBuilder setProtoFile(final Path path, final String content) {
    try {
      fileStorage.storeFile(path.toString(), content.getBytes());
    } catch (IOException e) {
      // TODO(staffan):
      throw new RuntimeException(e);
    }
    return this;
  }

  @Override
  public DescriptorSet buildDescriptor(final List<Path> paths) {
    if (paths.isEmpty()) {
      return DescriptorSet.empty();
    }

    try {
      final DescriptorProtos.FileDescriptorSet fileDescriptorSet = buildFileDescriptorSet(paths);
      return DescriptorSet.create(fileDescriptorSet, paths::contains);
    } catch (IOException | InterruptedException e) {
      // TODO(staffan):
      throw new RuntimeException(e);
    }
  }

  private DescriptorProtos.FileDescriptorSet buildFileDescriptorSet(final List<Path> paths)
      throws IOException, InterruptedException {
    final Path descriptorFilePath = Files.createTempFile("schema-registry-descriptor-", ".pb");
    try {
      final List<String> command = Lists.newArrayList();

      command.add("protoc");
      command.add("-o");
      command.add(descriptorFilePath.toAbsolutePath().toString());
      command.add("--include_source_info");
      command.add("--include_imports");

      paths.stream().map(Path::toString).forEach(command::add);

      final Process process =
          new ProcessBuilder(command).directory(fileStorage.root().toFile()).inheritIO().start();

      final int exitCode = process.waitFor();

      if (exitCode == 0) {
        try (final InputStream is = Files.newInputStream(descriptorFilePath)) {
          return DescriptorProtos.FileDescriptorSet.parseFrom(is);
        }
      } else {
        throw new IOException("Command had non-zero exit code: " + Joiner.on(' ').join(command));
      }
    } finally {
      Files.deleteIfExists(descriptorFilePath);
    }
  }

  @Override
  public void close() throws Exception {
    fileStorage.close();
  }
}
