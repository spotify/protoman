/*-
 * -\-\-
 * protoman-validation
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

package com.spotify.protoman.validation;

import com.google.auto.value.AutoValue;
import com.spotify.protoman.descriptor.GenericDescriptor;
import javax.annotation.Nullable;

@AutoValue
public abstract class ValidationViolation {

  public abstract ViolationType type();

  public abstract String description();

  @Nullable public abstract GenericDescriptor current();

  @Nullable public abstract GenericDescriptor candidate();

  public static ValidationViolation.Builder builder() {
    return new AutoValue_ValidationViolation.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setType(ViolationType type);

    public abstract Builder setDescription(String description);

    public abstract Builder setCurrent(GenericDescriptor current);

    public abstract Builder setCandidate(GenericDescriptor candidate);

    public abstract ValidationViolation build();
  }
}
