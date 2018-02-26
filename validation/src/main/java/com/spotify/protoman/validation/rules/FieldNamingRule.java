package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class FieldNamingRule implements ValidationRule {

  private FieldNamingRule() {
  }

  public static FieldNamingRule create() {
    return new FieldNamingRule();
  }

  @Override
  public void fieldAdded(final Context ctx,
                         final FieldDescriptor candidate,
                         final MessageDescriptor candidateContainingMessage) {
    if (!CaseFormatUtil.isLowerSnakeCase(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "field name should be lower_snake_case"
      );
    }
  }
}
