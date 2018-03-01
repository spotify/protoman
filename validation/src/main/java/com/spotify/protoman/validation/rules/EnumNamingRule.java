package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class EnumNamingRule implements ComparingValidationRule {

  private EnumNamingRule() {
  }

  public static EnumNamingRule create() {
    return new EnumNamingRule();
  }

  @Override
  public void enumAdded(final ValidationContext ctx, final EnumDescriptor candidate) {
    if (!CaseFormatUtil.isUpperCamelCaseName(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "enum name should be UpperCamelCase"
      );
    }
  }
}
