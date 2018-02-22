package com.spotify.protoman.validation;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.DescriptorSet;

public interface SchemaValidator {

  ImmutableList<ValidationViolation> validate(DescriptorSet current, DescriptorSet candidate);
}
