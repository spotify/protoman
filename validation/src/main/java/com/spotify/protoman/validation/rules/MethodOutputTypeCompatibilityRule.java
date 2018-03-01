package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;
import java.util.Optional;

public class MethodOutputTypeCompatibilityRule implements ComparingValidationRule {

  private final TypeCompatibilityChecker checker;

  @FunctionalInterface
  interface TypeCompatibilityChecker {
    Optional<TypeCompatibility.TypeIncompatibility> checkTypeCompatibility(
        MessageDescriptor current, MessageDescriptor candidate);
  }

  private MethodOutputTypeCompatibilityRule(final TypeCompatibilityChecker checker) {
    this.checker = checker;
  }

  public static MethodOutputTypeCompatibilityRule create() {
    return create(TypeCompatibility::checkMessageTypeCompatibility);
  }

  static MethodOutputTypeCompatibilityRule create(final TypeCompatibilityChecker checker) {
    return new MethodOutputTypeCompatibilityRule(checker);
  }

  @Override
  public void methodChanged(final ValidationContext ctx,
                            final MethodDescriptor current,
                            final MethodDescriptor candidate) {
    final Optional<TypeCompatibility.TypeIncompatibility> incompatibility =
        checker.checkTypeCompatibility(current.outputType(), candidate.outputType());

    if (incompatibility.isPresent()) {
      incompatibility.ifPresent(typeIncompatibility -> ctx.report(
          typeIncompatibility.type(),
          "output type changed: " + typeIncompatibility.description()
      ));
    } else if (!Objects.equals(current.outputType().fullName(), candidate.outputType().fullName())) {
      // Changing the output type will change the generated source code
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "method output type changed"
      );
    }
  }
}
