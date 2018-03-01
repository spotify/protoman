package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class FieldRemovalRule implements ComparingValidationRule {

  private FieldRemovalRule() {
  }

  public static FieldRemovalRule create() {
    return new FieldRemovalRule();
  }

  @Override
  public void fieldRemoved(final ValidationContext ctx,
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
}
