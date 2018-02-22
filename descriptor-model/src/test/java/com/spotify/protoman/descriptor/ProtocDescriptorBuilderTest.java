package com.spotify.protoman.descriptor;

import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static com.spotify.hamcrest.pojo.IsPojo.pojo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

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

    final DescriptorBuilder.Result result = sut.buildDescriptor(Stream.of(path1, path2));

    assertThat(
        result.compilationError(),
        is(nullValue())
    );

    assertThat(
        result.descriptorSet().fileDescriptors(),
        hasSize(2)
    );
  }

  @Test
  public void testBuildDescriptor_ensureDependenciesIncluded() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .build()
        .newDescriptorBuilder();

    // We're creating the descriptor for 1.proto, but add 2.proto as well since it's a dependency
    // of 1.proto
    final Path path1 = Paths.get("foo/bar/1.proto");
    final Path path2 = Paths.get("foo/bar/2.proto");
    sut.setProtoFile(
        path1,
        "syntax = 'proto2'; import 'foo/bar/2.proto'; message One { optional Two two = 1; }"
    );
    sut.setProtoFile(path2, "syntax = 'proto3'; message Two {}");

    final DescriptorBuilder.Result result = sut.buildDescriptor(Stream.of(path1));

    assertThat(
        result.descriptorSet().fileDescriptors(),
        hasSize(1)
    );

    assertThat(
        result.descriptorSet().findFileByPath(path1)
            .get()
            .findMessageByName("One")
            .findFieldByName("two")
            .messageType()
            .name(),
        equalTo("Two")
    );
  }

  @Test
  public void testBuildDescriptor_ensureSourceCodeInfoIncluded() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .build()
        .newDescriptorBuilder();

    final Path path = Paths.get("foo/bar/abc.proto");
    sut.setProtoFile(
        path,
        "syntax = 'proto3';\n"
        + "// It's an awesome type\n"
        + "message MyCoolType {\n"
        + "  int32 a_field = 1;\n"
        + "}"
    );

    final DescriptorBuilder.Result result = sut.buildDescriptor(Stream.of(path));
    final MessageDescriptor typeDescriptor =
        result.descriptorSet().findFileByPath(path).get().findMessageByName("MyCoolType");
    assertThat(
        typeDescriptor.sourceCodeInfo(),
        optionalWithValue(
            pojo(SourceCodeInfo.class)
                .where("leadingComments", equalTo(" It's an awesome type\n"))
        )
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

    final DescriptorBuilder.Result result = sut.buildDescriptor(Stream.of(path));

    assertThat(
        result.descriptorSet().fileDescriptors(),
        hasSize(1)
    );
  }

  @Test
  public void testBuildDescriptor_noInput() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .build()
        .newDescriptorBuilder();

    sut.setProtoFile(Paths.get("foo/bar/qux.proto"), "syntax = 'proto3';");

    final DescriptorBuilder.Result result = sut.buildDescriptor(Stream.empty());
    assertThat(
        result.descriptorSet().fileDescriptors(),
        hasSize(0)
    );
  }

  @Test
  public void testBuildDescriptor_protocNonZeroExitCode() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .build()
        .newDescriptorBuilder();
    sut.setProtoFile(Paths.get("foo/bar/qux.proto"), "syntax = 'proto3'; derp");

    final DescriptorBuilder.Result result = sut.buildDescriptor(
        Stream.of(Paths.get("foo/bar/qux.proto"))
    );

    assertThat(
        result.compilationError(),
        containsString("Expected top-level statement")
    );
  }

  @Test
  public void testBuildDescriptor_protocNotFound() throws Exception {
    final DescriptorBuilder sut = ProtocDescriptorBuilder.factoryBuilder()
        .protocPath(Paths.get("/herp/derp/not-protoc"))
        .build()
        .newDescriptorBuilder();
    sut.setProtoFile(Paths.get("foo/bar/qux.proto"), "syntax = 'proto3';");

    exception.expect(DescriptorBuilderException.class);

    sut.buildDescriptor(Stream.of(Paths.get("foo/bar/qux.proto")));
  }
}
