/*-
 * -\-\-
 * protoman-testutil
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

package com.spotify.protoman.testutil;

import com.google.auto.value.AutoValue;
import com.spotify.protoman.descriptor.DescriptorSet;

@AutoValue
public abstract class DescriptorSetPair {

  public abstract DescriptorSet current();

  public abstract DescriptorSet candidate();

  static DescriptorSetPair create(final DescriptorSet current, final DescriptorSet candidate) {
    return new AutoValue_DescriptorSetPair(current, candidate);
  }
}
