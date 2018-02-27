package com.spotify.protoman.validation.rules;

import com.google.common.base.CaseFormat;
import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class EnumDefaultRule implements ComparingValidationRule {

  private EnumDefaultRule() {
  }

  public static EnumDefaultRule create() {
    return new EnumDefaultRule();
  }

  @Override
  public void enumValueAdded(final ValidationContext ctx,
                             final EnumValueDescriptor candidate) {
    // NOTE(staffan): 'option allow_alias = true;' f's this up as implemented here, so it's
    // disabled for now. When allow_alias is enabled, there can be several values that has number
    // 0. Also, looking at googleapis the don't seem to always seem to follow this "rule" so
    // perhaps we shouldn't enforce it? E.g.:
    // https://github.com/googleapis/googleapis/blob/master/google/monitoring/v3/metric_service.proto#L202
    //
    // Value 0 should be used for unknown enum value
    /*if (candidate.number() == 0) {
      final String lowerCaseName = candidate.name().toLowerCase();
      if (!lowerCaseName.contains("unknown") && !lowerCaseName.contains("unspecified")) {
        ctx.report(
            ViolationType.BEST_PRACTICE_VIOLATION,
            "enum value 0 should be used for unknown value, e.g. UNKNOWN_"
            + candidate.getType().name().toUpperCase()
        );
      }
    }*/
  }

  @Override
  public void enumAdded(final ValidationContext ctx, final EnumDescriptor candidate) {
    validateDefaultValue(ctx, candidate);
  }

  @Override
  public void enumChanged(final ValidationContext ctx, final EnumDescriptor current,
                          final EnumDescriptor candidate) {
    validateDefaultValue(ctx, candidate);
  }

  private static void validateDefaultValue(final ValidationContext ctx, final EnumDescriptor descriptor) {
    // If "option allow_alias = true;" is used there can be multiple values which number 0.
    // Find values with number 0 that contains UNKNOWN or UNSPECIFIED and warn if there are none.
    if (descriptor.findValuesByNumber(0)
        .filter(value -> {
          final String lowerCaseName = value.name().toUpperCase();
          return lowerCaseName.contains("UNKNOWN") || lowerCaseName.contains("UNSPECIFIED");
        }).count() < 1) {
      final String suggestedName =
          CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, descriptor.name())
          + "_UNSPECIFIED";
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "enum value 0 should be used for unknown value, e.g. " + suggestedName
      );
    }
  }
}
