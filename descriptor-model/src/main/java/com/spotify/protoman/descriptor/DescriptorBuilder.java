package com.spotify.protoman.descriptor;

import com.google.auto.value.AutoValue;
import com.google.protobuf.DescriptorProtos;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface DescriptorBuilder extends AutoCloseable {


  DescriptorBuilder setProtoFile(Path path, String content) throws DescriptorBuilderException;

  /**
   * Can safely be called multiple times (you can call addProtoFile inbetween).
   */
  Result buildDescriptor(Stream<Path> paths) throws DescriptorBuilderException;

  @Override
  void close();

  interface Factory {

    DescriptorBuilder newDescriptorBuilder();
  }

  @AutoValue
  abstract class Result {

    @Nullable public abstract DescriptorProtos.FileDescriptorSet fileDescriptorSet();

    // TODO(staffan): Change this into a collection of more structured types?
    @Nullable public abstract String compilationError();

    public static Result create(final DescriptorProtos.FileDescriptorSet fileDescriptorSet) {
      return new AutoValue_DescriptorBuilder_Result(fileDescriptorSet, null);
    }

    public static Result error(final String compilationError) {
      return new AutoValue_DescriptorBuilder_Result(null, compilationError);
    }
  }
}
