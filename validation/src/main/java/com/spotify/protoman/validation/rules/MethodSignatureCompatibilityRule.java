package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;
import java.util.Optional;

public class MethodSignatureCompatibilityRule implements ComparingValidationRule {

  private final TypeCompatibilityChecker checker;

  @FunctionalInterface
  interface TypeCompatibilityChecker {
    Optional<TypeCompatibility.TypeIncompatibility> checkTypeCompatibility(
        MessageDescriptor current, MessageDescriptor candidate);
  }

  private MethodSignatureCompatibilityRule(final TypeCompatibilityChecker checker) {
    this.checker = checker;
  }

  public static MethodSignatureCompatibilityRule create() {
    return create(TypeCompatibility::checkMessageTypeCompatibility);
  }

  static MethodSignatureCompatibilityRule create(final TypeCompatibilityChecker checker) {
    return new MethodSignatureCompatibilityRule(checker);
  }

  @Override
  public void methodChanged(final ValidationContext ctx,
                            final MethodDescriptor current,
                            final MethodDescriptor candidate) {
    // Check input type
    if (!Objects.equals(current.inputType().fullName(), candidate.inputType().fullName())) {
      // Changing the input type will change the generated source code
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "method input type changed"
      );

      // ... and break wire-compatibility unless the new type is wire-compatible with the old type
      checker.checkTypeCompatibility(current.inputType(), candidate.inputType())
          .ifPresent(typeIncompatibility -> ctx.report(
              ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
              "wire-incompatible input type change: " + typeIncompatibility.description()
          ));
    }

    // Check output type
    if (!Objects.equals(current.outputType().fullName(), candidate.outputType().fullName())) {
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "method input type changed"
      );

      checker.checkTypeCompatibility(current.outputType(), candidate.outputType())
          .ifPresent(typeIncompatibility -> ctx.report(
              ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
              "wire-incompatible output type change: " + typeIncompatibility.description()
          ));
    }

    // TODO(staffan): is this wire compatible?
    if (current.toProto().getClientStreaming() != candidate.toProto().getClientStreaming()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "changed to/from client streaming"
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
