package com.spotify.protoman.validation.rules;

import com.google.common.base.CaseFormat;
import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;

public class EnumDefaultValueRule implements ValidationRule {

  private EnumDefaultValueRule() {
  }

  public static EnumDefaultValueRule create() {
    return new EnumDefaultValueRule();
  }

  @Override
  public void validateEnum(final ValidationContext ctx, final EnumDescriptor candidate) {
    // If "option allow_alias = true;" is used there can be multiple values which number 0.
    // Find values with number 0 that contains UNKNOWN or UNSPECIFIED and warn if there are none.
    if (candidate.findValuesByNumber(0)
            .filter(value -> {
              final String lowerCaseName = value.name().toUpperCase();
              return lowerCaseName.contains("UNKNOWN") || lowerCaseName.contains("UNSPECIFIED");
            }).count() < 1) {
      final String suggestedName =
          CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, candidate.name())
          + "_UNSPECIFIED";
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "enum value 0 should be used for unknown value, e.g. " + suggestedName
      );
    }
  }
}
