package com.spotify.protoman.validation.rules;

import com.google.protobuf.DescriptorProtos;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class MethodIdempotencyChangeRule implements ComparingValidationRule {

  private MethodIdempotencyChangeRule() {
  }

  public static MethodIdempotencyChangeRule create() {
    return new MethodIdempotencyChangeRule();
  }

  @Override
  public void methodChanged(final ValidationContext ctx,
                            final MethodDescriptor current,
                            final MethodDescriptor candidate) {
    // Idempotency status should never be downgraded
    if (newIdempotencyLevelLessStrict(current.idempotencyLevel(), candidate.idempotencyLevel())) {
      // TODO(staffan): How to categorize this?
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "Idempotency level changed to less strict"
      );
    }
  }

  private static boolean newIdempotencyLevelLessStrict(
      final DescriptorProtos.MethodOptions.IdempotencyLevel from,
      final DescriptorProtos.MethodOptions.IdempotencyLevel to) {
    return !(from == to
             || from == DescriptorProtos.MethodOptions.IdempotencyLevel.IDEMPOTENCY_UNKNOWN
             || (from == DescriptorProtos.MethodOptions.IdempotencyLevel.IDEMPOTENT
                 && to == DescriptorProtos.MethodOptions.IdempotencyLevel.NO_SIDE_EFFECTS));

  }
}
