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
