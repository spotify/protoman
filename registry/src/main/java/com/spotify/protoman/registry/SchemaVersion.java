package com.spotify.protoman.registry;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SchemaVersion {

  public abstract int major();

  public abstract int minor();

  public abstract int patch();

  @Override
  public String toString() {
    return new StringBuilder()
        .append(major())
        .append('.')
        .append(minor())
        .append('.')
        .append(patch())
        .toString();
  }

  public static SchemaVersion create(final int major, final int minor, final int patch) {
    return new AutoValue_SchemaVersion(major, minor, patch);
  }
}
