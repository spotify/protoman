package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class EnumValueRemovalRule implements ComparingValidationRule {

  private EnumValueRemovalRule() {
  }

  public static EnumValueRemovalRule create() {
    return new EnumValueRemovalRule();
  }

  @Override
  public void enumValueRemoved(final ValidationContext ctx, final EnumValueDescriptor current) {
    ctx.report(
        ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
        "enum value removed"
    );
  }
}
