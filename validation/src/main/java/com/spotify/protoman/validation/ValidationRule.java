package com.spotify.protoman.validation;

import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.descriptor.OneofDescriptor;
import com.spotify.protoman.descriptor.ServiceDescriptor;

public interface ValidationRule {

  default void messageAdded(final Context ctx,
                            final MessageDescriptor candidate) {
  }

  default void messageRemoved(final Context ctx,
                              final MessageDescriptor current) {
  }

  default void messageChanged(final Context ctx,
                              final MessageDescriptor current,
                              final MessageDescriptor candidate) {
  }

  default void fieldAdded(final Context ctx,
                          final FieldDescriptor candidate,
                          final MessageDescriptor candidateContainingMessage) {
  }

  default void fieldRemoved(final Context ctx,
                            final FieldDescriptor current,
                            final MessageDescriptor candidateContainingMessage) {
  }

  default void fieldChanged(final Context ctx,
                            final FieldDescriptor current,
                            final FieldDescriptor candidate) {
  }

  default void enumAdded(final Context ctx,
                         final EnumDescriptor candidate) {
  }

  default void enumRemoved(final Context ctx,
                           final EnumDescriptor current) {
  }

  default void enumChanged(final Context ctx,
                           final EnumDescriptor current,
                           final EnumDescriptor candidate) {
  }

  default void enumValueAdded(final Context ctx,
                              final EnumValueDescriptor candidate) {
  }

  default void enumValueRemoved(final Context ctx,
                                final EnumValueDescriptor current) {
  }

  default void enumValueChanged(final Context ctx,
                                final EnumValueDescriptor current,
                                final EnumValueDescriptor candidate) {
  }

  default void oneofAdded(final Context ctx,
                          final OneofDescriptor candidate) {
  }

  default void oneofRemoved(final Context ctx,
                            final OneofDescriptor current) {
  }

  default void oneofChanged(final Context ctx,
                            final OneofDescriptor current,
                            final OneofDescriptor candidate) {
  }

  default void serviceAdded(final Context ctx,
                            final ServiceDescriptor candidate) {
  }

  default void serviceRemoved(final Context ctx,
                              final ServiceDescriptor current) {
  }

  default void serviceChanged(final Context ctx,
                              final ServiceDescriptor current,
                              final ServiceDescriptor candidate) {
  }

  default void methodAdded(final Context ctx,
                           final MethodDescriptor candidate) {
  }

  default void methodRemoved(final Context ctx,
                             final MethodDescriptor current) {
  }

  default void methodChanged(final Context ctx,
                             final MethodDescriptor current,
                             final MethodDescriptor candidate) {
  }

  default void fileAdded(final Context ctx,
                         final FileDescriptor candidate) {
  }

  default void fileRemoved(final Context ctx,
                           final FileDescriptor current) {
  }

  default void fileChanged(final Context ctx,
                           final FileDescriptor current,
                           final FileDescriptor candidate) {
  }

  interface Context {

    void report(ViolationType type, String description);
  }
}

