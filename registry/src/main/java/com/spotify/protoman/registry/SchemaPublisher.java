/*-
 * -\-\-
 * protoman-registry
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.protoman.registry;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.protoman.validation.ValidationViolation;
import java.util.Optional;
import javax.annotation.Nullable;

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
      return error(error, ImmutableList.of());
    }

    public static PublishResult error(final String error,
                                      final ImmutableList<ValidationViolation> violations) {
      return new AutoValue_SchemaPublisher_PublishResult(
          Optional.of(error),
          violations,
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
