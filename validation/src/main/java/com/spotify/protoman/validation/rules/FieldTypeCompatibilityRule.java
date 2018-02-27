package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.FieldType;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;
import java.util.Optional;

public class FieldTypeCompatibilityRule implements ComparingValidationRule {

  private final FieldCompatibilityChecker checker;

  @FunctionalInterface
  interface FieldCompatibilityChecker {
    Optional<TypeCompatibility.TypeIncompatibility> checkFieldCompatibility(
        FieldDescriptor current, FieldDescriptor candidate);
  }

  private FieldTypeCompatibilityRule(final FieldCompatibilityChecker checker) {
    this.checker = checker;
  }

  public static FieldTypeCompatibilityRule create() {
    return create(TypeCompatibility::checkFieldTypeCompatibility);
  }

  static FieldTypeCompatibilityRule create(final FieldCompatibilityChecker checker) {
    return new FieldTypeCompatibilityRule(checker);
  }

  @Override
  public void fieldChanged(final ValidationContext ctx,
                           final FieldDescriptor current,
                           final FieldDescriptor candidate) {
    final Optional<TypeCompatibility.TypeIncompatibility> typeIncompatibility =
        checker.checkFieldCompatibility(current, candidate);
    // Is the new and old field type wire compatible?
    if (typeIncompatibility.isPresent()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "wire incompatible field type change: " + typeIncompatibility.get().description()
      );
    } else {
      if (current.type() != candidate.type()) {
        // ... if yes, are they the same type? If not, generated source code is likely
        // incompatible.
        // TODO(staffan): This isn't strictly true. Changing from int64 to int32 should be
        // benign in most languages. Fix to allow benign type changes.
        ctx.report(
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        );
      } else if (current.type() == FieldType.ENUM && !Objects.equals(
          current.enumType().fullName(), candidate.enumType().fullName())) {
        ctx.report(
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        );
      } else if (current.type() == FieldType.MESSAGE && !Objects.equals(
          current.messageType().fullName(), candidate.messageType().fullName())) {
        ctx.report(
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        );
      }
    }
  }
}
