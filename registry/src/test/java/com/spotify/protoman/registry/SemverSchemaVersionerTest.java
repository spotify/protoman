package com.spotify.protoman.registry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.nio.file.Path;
import java.nio.file.Paths;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class SemverSchemaVersionerTest {

  @Test
  @Parameters(method = "testParams")
  public void testDetermineVersion(final Path root,
                                   final String protoPackage,
                                   final SchemaVersion currentVersion,
                                   final SchemaVersion expectedVersion) throws Exception {
    final SemverSchemaVersioner sut = SemverSchemaVersioner.create();

    final SchemaRegistry.DescriptorSetPair descriptorSetPair = Util.buildDescriptorSetPair(root);

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
            SchemaVersion.create(1, 1, 0),
            SchemaVersion.create(1, 1, 1)
        },
        new Object[]{
            Paths.get("versioning/field_added"),
            "foo.bar",
            SchemaVersion.create(1, 3, 2),
            SchemaVersion.create(1, 4, 0)
        },
        new Object[]{
            Paths.get("versioning/formatting_change"),
            "foo.bar.v2",
            SchemaVersion.create(2, 3, 12),
            SchemaVersion.create(2, 3, 13)
        },
        new Object[]{
            Paths.get("versioning/ordering_change"),
            "foo.bar",
            SchemaVersion.create(1, 3, 12),
            SchemaVersion.create(1, 4, 0)
        },
        new Object[]{
            Paths.get("versioning/no_change"),
            "foo.bar",
            SchemaVersion.create(1, 3, 12),
            SchemaVersion.create(1, 3, 12)
        },
        new Object[]{
            Paths.get("versioning/new_package"),
            "foo.bar",
            null,
            SchemaVersion.create(1, 0, 0)
        },
    };
  }
}
