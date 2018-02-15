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
