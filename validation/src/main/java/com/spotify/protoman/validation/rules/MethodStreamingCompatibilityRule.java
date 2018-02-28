package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class MethodStreamingCompatibilityRule implements ComparingValidationRule {

  private MethodStreamingCompatibilityRule() {
  }

  public static MethodStreamingCompatibilityRule create() {
    return new MethodStreamingCompatibilityRule();
  }

  @Override
  public void methodChanged(final ValidationContext ctx,
                            final MethodDescriptor current,
                            final MethodDescriptor candidate) {
    // TODO(staffan): is this wire compatible?
    if (current.toProto().getClientStreaming() != candidate.toProto().getClientStreaming()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "changed to/from client streaxming"
      );
    }

    // TODO(staffan): is this wire compatible?
    if (current.toProto().getServerStreaming() != candidate.toProto().getServerStreaming()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "changed to/from server streaming"
      );
    }
  }
}
