package com.spotify.protoman.descriptor;

import com.google.auto.value.AutoValue;
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

    @Nullable public abstract DescriptorSet descriptorSet();

    // TODO(staffan): Change this into a collection of more structured types?
    @Nullable public abstract String compilationError();

    public static Result create(final DescriptorSet descriptorSet) {
      return new AutoValue_DescriptorBuilder_Result(descriptorSet, null);
    }

    public static Result error(final String compilationError) {
      return new AutoValue_DescriptorBuilder_Result(null, compilationError);
    }
  }
}
