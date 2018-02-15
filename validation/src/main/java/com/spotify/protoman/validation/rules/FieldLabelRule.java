package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;

/**
 * Verifies that field labels are not changed in an incompatible way.
 */
public class FieldLabelRule implements ValidationRule {

  private FieldLabelRule() {
  }

  public static FieldLabelRule create() {
    return new FieldLabelRule();
  }

  @Override
  public void fieldChanged(final Context ctx,
                           final FieldDescriptor current,
                           final FieldDescriptor candidate) {
    // If label is "required" changing it is wire-incompatible.
    if (current.isRequired() != candidate.isRequired()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "field changed to/from required"
      );
    }

    // Changing between repeated and optional is wire-compatible but will affect generated
    // source code.
    if (current.isRepeated() != candidate.isRepeated()) {
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "label changed"
      );
    }
  }
}
