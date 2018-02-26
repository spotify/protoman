package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class EnumNamingRule implements ValidationRule {

  private EnumNamingRule() {
  }

  public static EnumNamingRule create() {
    return new EnumNamingRule();
  }

  @Override
  public void enumAdded(final Context ctx, final EnumDescriptor candidate) {
    if (!CaseFormatUtil.isUpperCamelCaseName(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "enum name should be UpperCamelCase"
      );
    }
  }
}
