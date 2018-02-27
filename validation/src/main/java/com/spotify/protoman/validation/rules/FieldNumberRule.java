package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class FieldNumberRule implements ComparingValidationRule {

  private FieldNumberRule() {
  }

  public static FieldNumberRule create() {
    return new FieldNumberRule();
  }

  @Override
  public void fieldChanged(final ValidationContext ctx,
                           final FieldDescriptor current,
                           final FieldDescriptor candidate) {
    // Field number must not be changed
    if (candidate.number() != current.number()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "field number changed"
      );
    }
  }
}
