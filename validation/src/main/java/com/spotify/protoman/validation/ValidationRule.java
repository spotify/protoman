/*-
 * -\-\-
 * protoman-validation
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

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

