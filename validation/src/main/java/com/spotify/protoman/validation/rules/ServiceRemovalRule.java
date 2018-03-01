package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.ServiceDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class ServiceRemovalRule implements ComparingValidationRule {

  private ServiceRemovalRule() {
  }

  public static ServiceRemovalRule create() {
    return new ServiceRemovalRule();
  }

  @Override
  public void serviceRemoved(final ValidationContext ctx, final ServiceDescriptor current) {
    if (!current.options().getDeprecated()) {
      // TODO(staffan): is this "wire-incompatible"?!?!
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "service removed"
      );
    }
  }
}
