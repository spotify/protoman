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

@AutoValue
public abstract class SchemaVersion {

  public abstract String major();

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

  public static SchemaVersion create(final String major, final int minor, final int patch) {
    return new AutoValue_SchemaVersion(major, minor, patch);
  }
}
