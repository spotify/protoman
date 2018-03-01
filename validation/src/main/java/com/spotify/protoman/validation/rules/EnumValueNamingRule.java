package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class EnumValueNamingRule implements ComparingValidationRule {

  private EnumValueNamingRule() {
  }

  public static EnumValueNamingRule create() {
    return new EnumValueNamingRule();
  }

  @Override
  public void enumValueAdded(final ValidationContext ctx, final EnumValueDescriptor candidate) {
    if (!CaseFormatUtil.isUpperSnakeCase(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "enum value should be UPPER_SNAKE_CASE"
      );
    }
  }
}
