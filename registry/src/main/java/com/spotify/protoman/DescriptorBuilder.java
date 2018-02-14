package com.spotify.protoman;

import com.google.protobuf.DescriptorProtos;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DescriptorBuilder {

  DescriptorProtos.FileDescriptorSet buildDescriptor(Path root, List<Path> protos)
      throws IOException, InterruptedException;
}
