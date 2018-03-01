package com.spotify.protoman.descriptor;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.protobuf.DescriptorProtos;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

public class ProtocDescriptorBuilder implements DescriptorBuilder {

  private final Path protocPath;
  private final TemporaryFileStorage fileStorage;

  private ProtocDescriptorBuilder(final Path protocPath,
                                  final TemporaryFileStorage fileStorage) {
    this.protocPath = protocPath;
    this.fileStorage = fileStorage;
  }

  public static ProtocDescriptorBuilder create(final Path protocPath) {
    try {
      return new ProtocDescriptorBuilder(
          protocPath,
          TemporaryFileStorage.create("descriptor-builder-")
      );
    } catch (IOException e) {
      // TODO(staffan):
      throw new RuntimeException(e);
    }
  }

  public static Factory factory(final Path protocPath) {
    return () -> create(protocPath);
  }

  public static FactoryBuilder factoryBuilder() {
    return new FactoryBuilder();
  }

  @Override
  public DescriptorBuilder setProtoFile(final Path path, final String content)
      throws DescriptorBuilderException {
    try {
      fileStorage.storeFile(path.toString(), content.getBytes());
    } catch (IOException e) {
      throw new DescriptorBuilderException(e);
    }
    return this;
  }

  @Override
  public Result buildDescriptor(final Stream<Path> paths) throws DescriptorBuilderException {
    final ImmutableList<Path> pathsList = paths.collect(toImmutableList());
    if (pathsList.isEmpty()) {
      return Result.create(DescriptorProtos.FileDescriptorSet.getDefaultInstance());
    }

    try {
      return buildFileDescriptorSet(pathsList);
    } catch (InterruptedException | IOException e) {
      throw new DescriptorBuilderException(e);
    }
  }

  private Result buildFileDescriptorSet(final ImmutableList<Path> paths)
      throws IOException, InterruptedException {
    final Path descriptorFilePath = Files.createTempFile("schema-registry-descriptor-", ".pb");
    try {
      final List<String> command = Lists.newArrayList();

      command.add(protocPath.toString());
      command.add("-o");
      command.add(descriptorFilePath.toAbsolutePath().toString());
      command.add("--include_source_info");
      command.add("--include_imports");
      command.add("--error_format=gcc");

      paths.stream().map(Path::toString).forEach(command::add);

      final Process process = new ProcessBuilder(command)
          .directory(fileStorage.root().toFile())
          .start();

      final int exitCode = process.waitFor();

      if (exitCode == 0) {
        try (final InputStream is = Files.newInputStream(descriptorFilePath)) {
          final DescriptorProtos.FileDescriptorSet fileDescriptorSet =
              DescriptorProtos.FileDescriptorSet.parseFrom(is);
          return Result.create(fileDescriptorSet);
        }
      } else {
        return Result.error(CharStreams.toString(new InputStreamReader(process.getErrorStream())));
      }
    } finally {
      Files.deleteIfExists(descriptorFilePath);
    }
  }

  @Override
  public void close() {
    try {
      fileStorage.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static class FactoryBuilder {

    private Path protocPath = Paths.get("protoc");

    public FactoryBuilder protocPath(final Path protocPath) {
      this.protocPath = protocPath;
      return this;
    }

    public Factory build() {
      return factory(protocPath);
    }
  }
}
