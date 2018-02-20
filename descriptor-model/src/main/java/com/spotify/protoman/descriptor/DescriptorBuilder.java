package com.spotify.protoman.descriptor;

import java.nio.file.Path;
import java.util.List;

public interface DescriptorBuilder extends AutoCloseable {


  DescriptorBuilder setProtoFile(Path path, String content);

  /**
   * Can safely be called multiple times (you can call addProtoFile inbetween).
   */
  DescriptorSet buildDescriptor(List<Path> paths);

  interface Factory {

    DescriptorBuilder newDescriptorBuilder();
  }
}
