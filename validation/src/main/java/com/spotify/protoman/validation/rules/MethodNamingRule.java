package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class MethodNamingRule implements ComparingValidationRule {

  private MethodNamingRule() {
  }

  public static MethodNamingRule create() {
    return new MethodNamingRule();
  }
  @Override
  public void methodAdded(final ValidationContext ctx, final MethodDescriptor candidate) {
    if (!CaseFormatUtil.isUpperCamelCaseName(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "method name should be UpperCamelCase"
      );
    }
  }
}
