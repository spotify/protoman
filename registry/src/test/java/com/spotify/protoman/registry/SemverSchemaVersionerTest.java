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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.spotify.protoman.testutil.DescriptorSetPair;
import com.spotify.protoman.testutil.DescriptorSetUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class SemverSchemaVersionerTest {

  @Test
  public void testGetMajorVersion() throws Exception {
    assertThat(SemverSchemaVersioner.getMajorVersion("herp.derp"), equalTo("1"));
    assertThat(SemverSchemaVersioner.getMajorVersion("herp.derp.v1"), equalTo("1"));
    assertThat(SemverSchemaVersioner.getMajorVersion("herp.derp.v3"), equalTo("3"));
    assertThat(SemverSchemaVersioner.getMajorVersion("herp.derp.v1beta"), equalTo("1beta"));
    assertThat(SemverSchemaVersioner.getMajorVersion("herp.derp.v1beta2"), equalTo("1beta2"));
    assertThat(SemverSchemaVersioner.getMajorVersion("herp.derp.v1alpha"), equalTo("1alpha"));
    assertThat(SemverSchemaVersioner.getMajorVersion("herp.derp.v1alpha3"), equalTo("1alpha3"));
    assertThat(SemverSchemaVersioner.getMajorVersion("foo.v2.admin.v3"), equalTo("3"));
  }

  @Test
  @Parameters(method = "testParams")
  public void testDetermineVersion(final Path root,
                                   final String protoPackage,
                                   final SchemaVersion currentVersion,
                                   final SchemaVersion expectedVersion) throws Exception {
    final SemverSchemaVersioner sut = SemverSchemaVersioner.create();

    final DescriptorSetPair descriptorSetPair = DescriptorSetUtils.buildDescriptorSetPair(root);

    final SchemaVersion version = sut.determineVersion(
        protoPackage,
        currentVersion,
        descriptorSetPair.current(),
        descriptorSetPair.candidate()
    );

    assertThat(version, equalTo(expectedVersion));
  }

  private Object[] testParams() throws Exception {
    return new Object[]{
        new Object[]{
            Paths.get("versioning/comment_change"),
            "foo.bar",
            SchemaVersion.create("1", 1, 0),
            SchemaVersion.create("1", 1, 1)
        },
        new Object[]{
            Paths.get("versioning/field_added"),
            "foo.bar",
            SchemaVersion.create("1", 3, 2),
            SchemaVersion.create("1", 4, 0)
        },
        new Object[]{
            Paths.get("versioning/formatting_change"),
            "foo.bar.v2",
            SchemaVersion.create("2", 3, 12),
            SchemaVersion.create("2", 3, 13)
        },
        new Object[]{
            Paths.get("versioning/ordering_change"),
            "foo.bar.v1beta",
            SchemaVersion.create("1beta", 3, 12),
            SchemaVersion.create("1beta", 4, 0)
        },
        new Object[]{
            Paths.get("versioning/no_change"),
            "foo.bar.v1alpha2",
            SchemaVersion.create("1alpha2", 3, 12),
            SchemaVersion.create("1alpha2", 3, 12)
        },
        new Object[]{
            Paths.get("versioning/new_package"),
            "foo.bar",
            null,
            SchemaVersion.create("1", 0, 0)
        },
    };
  }
}
