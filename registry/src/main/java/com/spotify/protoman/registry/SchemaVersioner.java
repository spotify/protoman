package com.spotify.protoman.registry;

import com.spotify.protoman.descriptor.DescriptorSet;
import javax.annotation.Nullable;

public interface SchemaVersioner {

  SchemaVersion determineVersion(
      String protoPackage,
      @Nullable SchemaVersion currentVersion,
      DescriptorSet current, DescriptorSet candidate
  );
}
