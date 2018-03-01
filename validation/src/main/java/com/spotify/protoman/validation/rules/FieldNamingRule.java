package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class FieldNamingRule implements ComparingValidationRule {

  private FieldNamingRule() {
  }

  public static FieldNamingRule create() {
    return new FieldNamingRule();
  }

  @Override
  public void fieldAdded(final ValidationContext ctx, final FieldDescriptor candidate) {
    if (!CaseFormatUtil.isLowerSnakeCase(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "field name should be lower_snake_case"
      );
    }
  }
}
