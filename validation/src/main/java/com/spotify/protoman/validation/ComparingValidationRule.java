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
 * Interface for validation rules where both the present and the proposed schema can be inspected
 * when performing validation. This is allows, for example, ensuring that backwards compatibility
 * is maintained between versions.
 *
 * @see ValidationRule
 */
public interface ComparingValidationRule {

  default void messageAdded(final ValidationContext ctx,
                            final MessageDescriptor candidate) {
  }

  default void messageRemoved(final ValidationContext ctx,
                              final MessageDescriptor current) {
  }

  default void messageChanged(final ValidationContext ctx,
                              final MessageDescriptor current,
                              final MessageDescriptor candidate) {
  }

  default void fieldAdded(final ValidationContext ctx,
                          final FieldDescriptor candidate) {
  }

  default void fieldRemoved(final ValidationContext ctx,
                            final FieldDescriptor current,
                            final MessageDescriptor candidateContainingMessage) {
  }

  default void fieldChanged(final ValidationContext ctx,
                            final FieldDescriptor current,
                            final FieldDescriptor candidate) {
  }

  default void enumAdded(final ValidationContext ctx,
                         final EnumDescriptor candidate) {
  }

  default void enumRemoved(final ValidationContext ctx,
                           final EnumDescriptor current) {
  }

  default void enumChanged(final ValidationContext ctx,
                           final EnumDescriptor current,
                           final EnumDescriptor candidate) {
  }

  default void enumValueAdded(final ValidationContext ctx,
                              final EnumValueDescriptor candidate) {
  }

  default void enumValueRemoved(final ValidationContext ctx,
                                final EnumValueDescriptor current,
                                final EnumDescriptor currentContainingEnum) {
  }

  default void enumValueChanged(final ValidationContext ctx,
                                final EnumValueDescriptor current,
                                final EnumValueDescriptor candidate) {
  }

  default void oneofAdded(final ValidationContext ctx,
                          final OneofDescriptor candidate) {
  }

  default void oneofRemoved(final ValidationContext ctx,
                            final OneofDescriptor current) {
  }

  default void oneofChanged(final ValidationContext ctx,
                            final OneofDescriptor current,
                            final OneofDescriptor candidate) {
  }

  default void serviceAdded(final ValidationContext ctx,
                            final ServiceDescriptor candidate) {
  }

  default void serviceRemoved(final ValidationContext ctx,
                              final ServiceDescriptor current) {
  }

  default void serviceChanged(final ValidationContext ctx,
                              final ServiceDescriptor current,
                              final ServiceDescriptor candidate) {
  }

  default void methodAdded(final ValidationContext ctx,
                           final MethodDescriptor candidate) {
  }

  default void methodRemoved(final ValidationContext ctx,
                             final MethodDescriptor current) {
  }

  default void methodChanged(final ValidationContext ctx,
                             final MethodDescriptor current,
                             final MethodDescriptor candidate) {
  }

  default void fileAdded(final ValidationContext ctx,
                         final FileDescriptor candidate) {
  }

  default void fileRemoved(final ValidationContext ctx,
                           final FileDescriptor current) {
  }

  default void fileChanged(final ValidationContext ctx,
                           final FileDescriptor current,
                           final FileDescriptor candidate) {
  }

}

