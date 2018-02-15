package com.spotify.protoman.registry;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ProtocDescriptorBuilder implements DescriptorBuilder {
  private static final ProtocDescriptorBuilder INSTANCE = new ProtocDescriptorBuilder();

  private ProtocDescriptorBuilder() {}

  public static ProtocDescriptorBuilder create() {
    return INSTANCE;
  }

  @Override
  public DescriptorProtos.FileDescriptorSet buildDescriptor(
      final Path root, final List<Path> protos) throws IOException, InterruptedException {
    final Path descriptorFilePath = Files.createTempFile("schema-registry-descriptor-", ".pb");
    try {
      final List<String> command = Lists.newArrayList();

      command.add("protoc");
      command.add("-o");
      command.add(descriptorFilePath.toAbsolutePath().toString());
      command.add("--include_source_info");
      command.add("--include_imports");

      protos.stream().map(Path::toString).forEach(command::add);

      final Process process =
          new ProcessBuilder(command).directory(root.toFile()).inheritIO().start();

      final int exitCode = process.waitFor();

      if (exitCode == 0) {
        final DescriptorProtos.FileDescriptorSet fileDescriptorSet;

        try (InputStream is = Files.newInputStream(descriptorFilePath)) {
          fileDescriptorSet = DescriptorProtos.FileDescriptorSet.parseFrom(is);
        }

        return fileDescriptorSet;
      } else {
        throw new IOException("Command had non-zero exit code: " + Joiner.on(' ').join(command));
      }
    } finally {
      Files.deleteIfExists(descriptorFilePath);
    }
  }
}
