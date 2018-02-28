package com.spotify.protoman.validation.rules;

import com.google.auto.value.AutoValue;
import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.FieldType;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;
import java.util.Optional;

class TypeCompatibility {
  private TypeCompatibility() {
    // Prevent instantiation
  }

  static Optional<TypeIncompatibility> checkMessageTypeCompatibility(
      final MessageDescriptor current,
      final MessageDescriptor candidate) {
    if (Objects.equals(current.fullName(), candidate.fullName())) {
      // Message type is the same for current and candidate -- any incompatible changes to the
      // message type will be caught elsewhere.
      return Optional.empty();
    }

    for (final FieldDescriptor currentField : current.fields()) {
      final FieldDescriptor candidateField = candidate.findFieldByNumber(currentField.number());

      if (candidateField == null) {
        // Field with the same number is not present in the type replacing the current message ->
        // consider it incompatible
        return Optional.of(TypeIncompatibility.create(
            String.format(
                "message types %s and %s are not interchangable, field %s does exist in the new "
                + "message type used",
                current.name(), candidate.name(), currentField.name()),
            ViolationType.WIRE_INCOMPATIBILITY_VIOLATION
        ));
      }

      if (!Objects.equals(currentField.name(), candidateField.name())) {
        return Optional.of(TypeIncompatibility.create(
            String.format(
                "message types %s and %s are not interchangable, field %s has different name in "
                + "new message type used (current=%s, candidate=%s)",
                current.name(), candidate.name(), currentField.name(),
                currentField.name(), candidateField.name()),
            ViolationType.FIELD_MASK_INCOMPATIBILITY
        ));
      }

      final Optional<TypeIncompatibility> fieldTypeCompatibility =
          checkFieldTypeCompatibility(currentField, candidateField);
      if (fieldTypeCompatibility.isPresent()) {
        // One or more fields are type-incompatible
        return fieldTypeCompatibility;
      }

      if (!Objects.equals(currentField.jsonName(), candidateField.jsonName())) {
        return Optional.of(TypeIncompatibility.create(
            String.format(
                "message types %s and %s are not interchangable, field %s has different JSON "
                + "name in new message type used (current=%s, candidate=%s)",
                current.name(), candidate.name(), currentField.name(),
                currentField.jsonName(), candidateField.jsonName()),
            ViolationType.JSON_ENCODING_INCOMPATIBILITY
        ));
      }
    }

    return Optional.empty();
  }

  static Optional<TypeIncompatibility> checkEnumTypeCompatibility(
      final EnumDescriptor current, final EnumDescriptor candidate) {
    if (Objects.equals(current.fullName(), candidate.fullName())) {
      return Optional.empty();
    }

    for (final EnumValueDescriptor currentValue : current.values()) {
      // NOTE: We match values but number not name here! This allows for the value names to be
      // changed as long as the numbers are the same.
      // TODO(staffan): Changing the name will change the JSON encoding, which we should catch...
      final Optional<EnumValueDescriptor> candidateValue =
          candidate.findValuesByNumber(currentValue.number()).findFirst();

      if (!candidateValue.isPresent()) {
        // Value with the same number is not present in the type replacing the current enum ->
        // consider it incompatible
        return Optional.of(TypeIncompatibility.create(
            String.format(
                "enum types %s and %s are not interchangable, value with number %d does exist in "
                + "the new type used",
                current.name(), candidate.name(), currentValue.number()),
            ViolationType.WIRE_INCOMPATIBILITY_VIOLATION
        ));
      }
    }

    return Optional.empty();
  }

  static Optional<TypeIncompatibility> checkFieldTypeCompatibility(
      final FieldDescriptor current,
      final FieldDescriptor candidate) {
    final FieldType currentType = current.type();
    final FieldType candidateType = candidate.type();

    if (currentType == candidateType) {
      if (currentType == FieldType.MESSAGE) {
        return checkMessageTypeCompatibility(current.messageType(), candidate.messageType());
      }

      if (currentType == FieldType.ENUM) {
        return checkEnumTypeCompatibility(current.enumType(), candidate.enumType());
      }

      return Optional.empty();
    }

    if (!FieldType.isWireCompatible(currentType, candidateType)) {
      return Optional.of(TypeIncompatibility.create(
          String.format("wire-incompatible field type change %s -> %s", currentType, candidateType),
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION
      ));
    }

    return Optional.empty();
  }

  @AutoValue
  abstract static class TypeIncompatibility {

    abstract String description();

    abstract ViolationType type();

    static TypeIncompatibility create(final String description,
                                      final ViolationType type) {
      return new AutoValue_TypeCompatibility_TypeIncompatibility(description, type);
    }
  }
}
