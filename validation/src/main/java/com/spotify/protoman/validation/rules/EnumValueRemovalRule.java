package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.EnumDescriptor;
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
  public void enumValueRemoved(final ValidationContext ctx, final EnumValueDescriptor current,
                               final EnumDescriptor candidateContainingEnum) {
    if (candidateContainingEnum.isReservedNumber(current.number())
        && candidateContainingEnum.isReservedName(current.name())) {
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "enum value made reserved"
      );
    } else {
      // TODO(staffan): Removing a field is tehnically okay, but we probably shouldn't encourage
      // it, as bad things will happen if the same field number is reused later. Warn at more
      // severe level?
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "enum value removed"
      );
    }
  }
}
