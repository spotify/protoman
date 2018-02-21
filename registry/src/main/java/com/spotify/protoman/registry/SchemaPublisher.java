package com.spotify.protoman.registry;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.protoman.validation.ValidationViolation;

public interface SchemaPublisher {

  PublishResult publishSchemata(ImmutableList<SchemaFile> schemaFiles);

  @AutoValue
  abstract class PublishResult {

    public abstract ImmutableList<ValidationViolation> violations();

    public abstract ImmutableMap<String, SchemaVersionPair> publishedPackages();

    public static PublishResult create(final ImmutableList<ValidationViolation> violations,
                                       ImmutableMap<String, SchemaVersionPair> publishedPackages) {
      return new AutoValue_SchemaPublisher_PublishResult(violations, publishedPackages);
    }
  }

  @AutoValue
  abstract class SchemaVersionPair {

    public abstract SchemaVersion version();

    public abstract SchemaVersion prevVersion();

    public static SchemaVersionPair create(final SchemaVersion version, SchemaVersion prevVersion) {
      return new AutoValue_SchemaPublisher_SchemaVersionPair(version, prevVersion);
    }
  }
}
