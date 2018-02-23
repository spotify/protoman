package com.spotify.protoman.registry;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.spotify.protoman.descriptor.DescriptorBuilder;
import com.spotify.protoman.descriptor.DescriptorBuilderException;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.descriptor.ProtocDescriptorBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

public class Util {

  private static final DescriptorBuilder.Factory DESCRIPTOR_BUILDER_FACTORY =
      ProtocDescriptorBuilder.factoryBuilder().build();

  private Util() {
    // Prevent instantiation
  }

  public static DescriptorSet buildDescriptorSet(final Path root)
      throws IOException, URISyntaxException, DescriptorBuilderException {
    return buildDescriptorSet(root, path -> true);
  }

  public static DescriptorSetPair buildDescriptorSetPair(final Path root)
      throws IOException, URISyntaxException, DescriptorBuilderException {
    final DescriptorSet current = buildDescriptorSet(root.resolve("current"));
    final DescriptorSet candidate = buildDescriptorSet(root.resolve("candidate"));
    return DescriptorSetPair.create(current, candidate);
  }

  public static DescriptorSet buildDescriptorSet(final Path root, final Predicate<Path> filter)
      throws IOException, URISyntaxException, DescriptorBuilderException {
    final ClassLoader classLoader = Util.class.getClassLoader();

    final DescriptorBuilder descriptorBuilder = DESCRIPTOR_BUILDER_FACTORY.newDescriptorBuilder();

    final ImmutableList<Path> resourceFiles = listResourceFiles(root);
    for (final Path path : resourceFiles) {
      final String strPath = root.resolve(path).toString();
      try (final InputStream is = classLoader.getResourceAsStream(strPath)) {
        final String content = CharStreams.toString(new InputStreamReader(is));
        descriptorBuilder.setProtoFile(path, content);
      }
    }

    final DescriptorBuilder.Result result = descriptorBuilder.buildDescriptor(
        resourceFiles.stream()
            .filter(filter)
    );

    if (result.compilationError() != null) {
      throw new RuntimeException("Failed to build descriptor set: " + result.compilationError());
    }

    return result.descriptorSet();
  }

  private static ImmutableList<Path> listResourceFiles(final Path root)
      throws URISyntaxException, IOException {
    final ClassLoader classLoader = Util.class.getClassLoader();
    final URL resource = classLoader.getResource(root.toString());
    if (resource == null) {
      return ImmutableList.of();
    }
    final URI uri = resource.toURI();
    final Path path;
    if (uri.getScheme().equals("jar")) {
      final FileSystem fileSystem = FileSystems.newFileSystem(uri, ImmutableMap.of());
      path = fileSystem.getPath(root.toString());
    } else {
      path = Paths.get(uri);
    }

    return Files.walk(path)
        .filter(Files::isRegularFile)
        .map(path::relativize)
        .collect(toImmutableList());
  }

  @AutoValue
  abstract static class DescriptorSetPair {

    abstract DescriptorSet current();

    abstract DescriptorSet candidate();

    public static DescriptorSetPair create(
        final DescriptorSet current, final DescriptorSet candidate) {
      return new AutoValue_Util_DescriptorSetPair(current, candidate);
    }
  }
}