package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class MessageNamingRule implements ComparingValidationRule {

  private MessageNamingRule() {
  }

  public static MessageNamingRule create() {
    return new MessageNamingRule();
  }

  @Override
  public void messageAdded(final ValidationContext ctx, final MessageDescriptor candidate) {
    if (!CaseFormatUtil.isUpperCamelCaseName(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "message name should be UpperCamelCase"
      );
    }
  }
}
