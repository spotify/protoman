package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class MessageRemovalRule implements ComparingValidationRule {

  private MessageRemovalRule() {
  }

  public static MessageRemovalRule create() {
    return new MessageRemovalRule();
  }

  @Override
  public void messageRemoved(final ValidationContext ctx, final MessageDescriptor current) {
    // Warn when removing non-deprecated messages
    if (!current.options().getDeprecated()) {
      // TODO(staffan): is this abi-incompatible?!?!
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "message removed"
      );
    }
  }
}
