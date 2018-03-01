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