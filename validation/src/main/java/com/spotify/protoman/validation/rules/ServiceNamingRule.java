package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.ServiceDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class ServiceNamingRule implements ComparingValidationRule {

  private ServiceNamingRule() {
  }

  public static ServiceNamingRule create() {
    return new ServiceNamingRule();
  }

  @Override
  public void serviceAdded(final ValidationContext ctx, final ServiceDescriptor candidate) {
    if (!CaseFormatUtil.isUpperCamelCaseName(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "service name should be UpperCamelCase"
      );
    }
  }
}
