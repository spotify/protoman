package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class EnumRemovalRule implements ComparingValidationRule {

  private EnumRemovalRule() {
  }

  public static EnumRemovalRule create() {
    return new EnumRemovalRule();
  }
  @Override
  public void enumRemoved(final ValidationContext ctx, final EnumDescriptor current) {
    // TODO(staffan): is this abi-incompatible?!?!
    ctx.report(
        ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
        "enum removed"
    );
  }
}
