package com.spotify.protoman.testutil;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

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
import java.util.stream.Stream;

public class DescriptorSetUtils {

  private static final DescriptorBuilder.Factory DESCRIPTOR_BUILDER_FACTORY =
      ProtocDescriptorBuilder.factoryBuilder().build();

  private DescriptorSetUtils() {
    // Prevent instantiation
  }

  /**
   * Convenience method to build a DescriptorSet for a single proto file.
   */
  public static DescriptorSet buildDescriptorSet(
      final String path, final String content) throws Exception {
    final DescriptorBuilder descriptorBuilder = DESCRIPTOR_BUILDER_FACTORY.newDescriptorBuilder();
    descriptorBuilder.setProtoFile(Paths.get(path), content);
    final DescriptorBuilder.Result result =
        descriptorBuilder.buildDescriptor(Stream.of(Paths.get(path)));
    if (result.compilationError() != null) {
      throw new RuntimeException("Protobuf compilation failed: " + result.compilationError());
    }
    checkNotNull(result.descriptorSet());
    return result.descriptorSet();
  }

  public static DescriptorSet buildDescriptorSet(final Path root)
      throws IOException, URISyntaxException, DescriptorBuilderException {
    return buildDescriptorSet(root, path -> true);
  }

  public static DescriptorSetPair buildDescriptorSetPair(final Path root)
      throws IOException, URISyntaxException, DescriptorBuilderException {
    return buildDescriptorSetPair(root, path -> true);
  }

  public static DescriptorSetPair buildDescriptorSetPair(final Path root,
                                                         final Predicate<Path> filter)
      throws IOException, URISyntaxException, DescriptorBuilderException {
    final DescriptorSet current = buildDescriptorSet(root.resolve("current"), filter);
    final DescriptorSet candidate = buildDescriptorSet(root.resolve("candidate"), filter);
    return DescriptorSetPair.create(current, candidate);
  }

  public static DescriptorSet buildDescriptorSet(final Path root, final Predicate<Path> filter)
      throws IOException, URISyntaxException, DescriptorBuilderException {
    final ClassLoader classLoader = DescriptorSetUtils.class.getClassLoader();

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
    final ClassLoader classLoader = DescriptorSetUtils.class.getClassLoader();
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

}