package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.OneofDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class OneofNamingRule implements ValidationRule {

  private OneofNamingRule() {
  }

  public static OneofNamingRule create() {
    return new OneofNamingRule();
  }

  @Override
  public void oneofAdded(final Context ctx, final OneofDescriptor candidate) {
    if (!CaseFormatUtil.isLowerSnakeCase(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "oneof name should be lower_snake_case"
      );
    }
  }
}
