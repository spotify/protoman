package com.spotify.protoman.validation;

import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.descriptor.OneofDescriptor;
import com.spotify.protoman.descriptor.ServiceDescriptor;

/**
 * Interface for rules that operate only on the proposed version of a schema.
 *
 * This is a more convenient but less powerful alternativ to {@link ComparingValidationRule}.
 *
 * @see ComparingValidationRule
 */
public interface ValidationRule {

  default void validateMessage(final ValidationContext ctx, final MessageDescriptor candidate) {
  }

  default void validateField(final ValidationContext ctx, final FieldDescriptor candidate) {
  }

  default void validateEnum(final ValidationContext ctx, final EnumDescriptor candidate) {
  }

  default void validateEnumValue(final ValidationContext ctx, final EnumValueDescriptor candidate) {
  }

  default void validateOneof(final ValidationContext ctx, final OneofDescriptor candidate) {
  }

  default void validateService(final ValidationContext ctx, final ServiceDescriptor candidate) {
  }

  default void validateMethod(final ValidationContext ctx, final MethodDescriptor candidate) {
  }

  default void validateFile(final ValidationContext ctx, final FileDescriptor candidate) {
  }
}

