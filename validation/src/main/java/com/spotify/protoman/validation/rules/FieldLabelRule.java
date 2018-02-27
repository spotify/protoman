package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

/**
 * Verifies that field labels are not changed in an incompatible way.
 */
public class FieldLabelRule implements ComparingValidationRule {

  private FieldLabelRule() {
  }

  public static FieldLabelRule create() {
    return new FieldLabelRule();
  }

  @Override
  public void fieldChanged(final ValidationContext ctx,
                           final FieldDescriptor current,
                           final FieldDescriptor candidate) {
    // If label is "required" changing it is wire-incompatible.
    if (current.isRequired() != candidate.isRequired()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "field label changed to/from required"
      );
    } else if (current.isRepeated() != candidate.isRepeated()) {
      // Changing between repeated and optional is wire-compatible but will affect generated
      // source code.
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "field label changed"
      );
    }
  }
}
