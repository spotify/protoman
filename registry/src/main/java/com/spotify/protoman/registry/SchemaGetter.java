package com.spotify.protoman.registry;

import com.google.common.collect.ImmutableList;
import java.util.stream.Stream;

public interface SchemaGetter {

  // TODO(staffan): Should allow getting packages at specific versions
  Stream<SchemaFile> getSchemataForPackages(ImmutableList<String> protoPackages);
}
