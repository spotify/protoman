package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class MethodRemovalRule implements ComparingValidationRule {

  private MethodRemovalRule() {
  }

  public static MethodRemovalRule create() {
    return new MethodRemovalRule();
  }

  @Override
  public void methodRemoved(final ValidationContext ctx, final MethodDescriptor current) {
    // TODO(staffan): Take things like lifecycle into account here. Removing methods from a GA
    // service is a no-no but removing it from a experimental service is probably okay.
    // Would be very cool if we could get the number of users of a service and base decision off of
    // that.
    if (!current.options().getDeprecated()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "method removed"
      );
    }
  }
}
