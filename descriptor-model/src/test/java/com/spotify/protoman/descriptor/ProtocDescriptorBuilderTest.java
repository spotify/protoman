package com.spotify.protoman.descriptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ProtocDescriptorBuilderTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void testBuildDescriptor() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .build()
        .newDescriptorBuilder();

    final Path path1 = Paths.get("foo/bar/1.proto");
    final Path path2 = Paths.get("foo/bar/2.proto");
    sut.setProtoFile(path1, "syntax = 'proto2'; message One {}");
    sut.setProtoFile(path2, "syntax = 'proto3'; message Two {}");

    final DescriptorSet descriptorSet = sut.buildDescriptor(Stream.of(path1, path2));

    assertThat(
        descriptorSet.fileDescriptors(),
        hasSize(2)
    );
  }

  // Verify that setProtoFile called multiple times for the same path overwrites the previous
  // content
  @Test
  public void testBuildDescriptor_overwriteFiles() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .build()
        .newDescriptorBuilder();
    final Path path = Paths.get("foo/bar/qux.proto");
    sut.setProtoFile(path, "derp");
    sut.setProtoFile(path, "syntax = 'proto3';");

    final DescriptorSet descriptorSet = sut.buildDescriptor(Stream.of(path));

    assertThat(
        descriptorSet.fileDescriptors(),
        hasSize(1)
    );
  }

  @Test
  public void testBuildDescriptor_noInput() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .build()
        .newDescriptorBuilder();
    final DescriptorSet descriptorSet = sut.buildDescriptor(Stream.empty());
    assertThat(
        descriptorSet.fileDescriptors(),
        hasSize(0)
    );
  }

  @Test
  public void testBuildDescriptor_protocNonZeroExitCode() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .build()
        .newDescriptorBuilder();
    sut.setProtoFile(Paths.get("foo/bar/qux.proto"), "derp");

    exception.expect(RuntimeException.class);

    final DescriptorSet descriptorSet = sut.buildDescriptor(
        Stream.of(Paths.get("foo/bar/qux.proto"))
    );
  }

  @Test
  public void testBuildDescriptor_protocNotFound() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .protocPath(Paths.get("/herp/derp/not-protoc"))
        .build()
        .newDescriptorBuilder();
    sut.setProtoFile(Paths.get("foo/bar/qux.proto"), "syntax = 'proto3';");

    exception.expect(RuntimeException.class);

    final DescriptorSet descriptorSet = sut.buildDescriptor(
        Stream.of(Paths.get("foo/bar/qux.proto"))
    );
  }
}
