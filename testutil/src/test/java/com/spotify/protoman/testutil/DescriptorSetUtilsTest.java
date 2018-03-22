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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.nio.file.Paths;
import org.junit.Test;

public class DescriptorSetUtilsTest {

  @Test
  public void testBuildDescriptorSetPair() throws Exception {
    final DescriptorSetPair descriptorSetPair =
        DescriptorSetUtils.buildDescriptorSetPair(Paths.get(""));

    assertThat(
        descriptorSetPair.current().fileDescriptors(),
        hasSize(1)
    );

    assertThat(
        descriptorSetPair.candidate().fileDescriptors(),
        hasSize(2)
    );
  }

  @Test
  public void testBuildDescriptorSetPair_filtering() throws Exception {
    final DescriptorSetPair descriptorSetPair = DescriptorSetUtils.buildDescriptorSetPair(
        Paths.get(""),
        path -> !Paths.get("foo/bar/baz.proto").equals(path)
    );

    assertThat(
        descriptorSetPair.current().fileDescriptors(),
        hasSize(0)
    );

    assertThat(
        descriptorSetPair.candidate().fileDescriptors(),
        hasSize(1)
    );
  }
}