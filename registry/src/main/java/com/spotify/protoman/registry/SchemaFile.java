package com.spotify.protoman.registry;

import com.google.auto.value.AutoValue;
import java.nio.file.Path;

@AutoValue
public abstract class SchemaFile {

  public abstract Path path();

  public abstract String content();

  public static SchemaFile create(final Path path, final String content) {
    return new AutoValue_SchemaFile(path, content);
  }
}
