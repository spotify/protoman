package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.descriptor.ServiceDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;

public class RemovalRule implements ValidationRule {

  private RemovalRule() {
  }

  public static RemovalRule create() {
    return new RemovalRule();
  }

  @Override
  public void messageRemoved(final Context ctx, final MessageDescriptor current) {
    // Warn when removing non-deprecated messages
    if (!current.options().getDeprecated()) {
      // TODO(staffan): is this abi-incompatible?!?!
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "message removed"
      );
    }
  }

  @Override
  public void fieldRemoved(final Context ctx,
                           final FieldDescriptor current,
                           final MessageDescriptor candidateContainingMessage) {
    if (candidateContainingMessage.isReservedNumber(current.number())
        && candidateContainingMessage.isReservedName(current.name())) {
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "field made reserved"
      );
    } else {
      // TODO(staffan): Removing a field is tehnically okay, but we probably shouldn't encourage
      // it, as bad things will happen if the same field number is reused later. Warn at more
      // severe level?
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "field removed"
      );
    }
  }

  @Override
  public void enumRemoved(final Context ctx, final EnumDescriptor current) {
    // TODO(staffan): is this abi-incompatible?!?!
    ctx.report(
        ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
        "enum removed"
    );
  }

  @Override
  public void enumValueRemoved(final Context ctx, final EnumValueDescriptor current) {
    ctx.report(
        ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
        "enum value removed"
    );
  }

  @Override
  public void serviceRemoved(final Context ctx, final ServiceDescriptor current) {
    if (!current.options().getDeprecated()) {
      // TODO(staffan): is this "wire-incompatible"?!?!
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "service removed"
      );
    }
  }

  @Override
  public void methodRemoved(final Context ctx, final MethodDescriptor current) {
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
