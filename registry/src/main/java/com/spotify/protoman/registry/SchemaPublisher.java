package com.spotify.protoman.registry;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.protoman.validation.ValidationViolation;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.text.html.Option;

public interface SchemaPublisher {

  PublishResult publishSchemata(ImmutableList<SchemaFile> schemaFiles);

  @AutoValue
  abstract class PublishResult {

    public abstract Optional<String> error();

    public abstract ImmutableList<ValidationViolation> violations();

    public abstract ImmutableMap<String, SchemaVersionPair> publishedPackages();

    public static PublishResult create(final ImmutableList<ValidationViolation> violations,
                                       ImmutableMap<String, SchemaVersionPair> publishedPackages) {
      return new AutoValue_SchemaPublisher_PublishResult(
          Optional.empty(),
          violations,
          publishedPackages
      );
    }

    public static PublishResult error(final String error) {
      return new AutoValue_SchemaPublisher_PublishResult(
          Optional.of(error),
          ImmutableList.of(),
          ImmutableMap.of()
      );
    }
  }

  @AutoValue
  abstract class SchemaVersionPair {

    public abstract SchemaVersion version();

    public abstract Optional<SchemaVersion> prevVersion();

    public static SchemaVersionPair create(final SchemaVersion version,
                                           @Nullable final SchemaVersion prevVersion) {
      return new AutoValue_SchemaPublisher_SchemaVersionPair(
          version, Optional.ofNullable(prevVersion)
      );
    }
  }
}
