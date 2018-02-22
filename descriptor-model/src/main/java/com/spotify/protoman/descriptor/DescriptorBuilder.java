package com.spotify.protoman.descriptor;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface DescriptorBuilder extends AutoCloseable {


  DescriptorBuilder setProtoFile(Path path, String content);

  /**
   * Can safely be called multiple times (you can call addProtoFile inbetween).
   */
  DescriptorSet buildDescriptor(Stream<Path> paths);

  interface Factory {

    DescriptorBuilder newDescriptorBuilder();
  }
}
