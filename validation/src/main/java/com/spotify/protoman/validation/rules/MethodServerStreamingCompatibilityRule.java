package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class MethodServerStreamingCompatibilityRule implements ComparingValidationRule {

  private MethodServerStreamingCompatibilityRule() {
  }

  public static MethodServerStreamingCompatibilityRule create() {
    return new MethodServerStreamingCompatibilityRule();
  }

  @Override
  public void methodChanged(final ValidationContext ctx,
                            final MethodDescriptor current,
                            final MethodDescriptor candidate) {
    // TODO(staffan): is this wire compatible?
    if (current.toProto().getServerStreaming() != candidate.toProto().getServerStreaming()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "changed to/from server streaming"
      );
    }
  }
}
