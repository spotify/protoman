package com.spotify.protoman.validation;

import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.descriptor.OneofDescriptor;
import com.spotify.protoman.descriptor.ServiceDescriptor;

class RuleAdapter implements ComparingValidationRule {

  private final ValidationRule adaptee;

  private RuleAdapter(final ValidationRule adaptee) {
    this.adaptee = adaptee;
  }

  public static ComparingValidationRule adapt(final ValidationRule rule) {
    return new RuleAdapter(rule);
  }

  @Override
  public void messageAdded(final ValidationContext ctx, final MessageDescriptor candidate) {
    adaptee.validateMessage(ctx, candidate);
  }

  @Override
  public void messageRemoved(final ValidationContext ctx, final MessageDescriptor current) {
  }

  @Override
  public void messageChanged(final ValidationContext ctx, final MessageDescriptor current,
                             final MessageDescriptor candidate) {
    adaptee.validateMessage(ctx, candidate);
  }

  @Override
  public void fieldAdded(final ValidationContext ctx, final FieldDescriptor candidate,
                         final MessageDescriptor candidateContainingMessage) {
    adaptee.validateField(ctx, candidate);
  }

  @Override
  public void fieldRemoved(final ValidationContext ctx, final FieldDescriptor current,
                           final MessageDescriptor candidateContainingMessage) {
  }

  @Override
  public void fieldChanged(final ValidationContext ctx, final FieldDescriptor current,
                           final FieldDescriptor candidate) {
    adaptee.validateField(ctx, candidate);
  }

  @Override
  public void enumAdded(final ValidationContext ctx, final EnumDescriptor candidate) {
    adaptee.validateEnum(ctx, candidate);
  }

  @Override
  public void enumRemoved(final ValidationContext ctx, final EnumDescriptor current) {
  }

  @Override
  public void enumChanged(final ValidationContext ctx, final EnumDescriptor current,
                          final EnumDescriptor candidate) {
    adaptee.validateEnum(ctx, candidate);
  }

  @Override
  public void enumValueAdded(final ValidationContext ctx, final EnumValueDescriptor candidate) {
    adaptee.validateEnumValue(ctx, candidate);
  }

  @Override
  public void enumValueRemoved(final ValidationContext ctx, final EnumValueDescriptor current) {
  }

  @Override
  public void enumValueChanged(final ValidationContext ctx, final EnumValueDescriptor current,
                               final EnumValueDescriptor candidate) {
    adaptee.validateEnumValue(ctx, candidate);
  }

  @Override
  public void oneofAdded(final ValidationContext ctx, final OneofDescriptor candidate) {
    adaptee.validateOneof(ctx, candidate);
  }

  @Override
  public void oneofRemoved(final ValidationContext ctx, final OneofDescriptor current) {
  }

  @Override
  public void oneofChanged(final ValidationContext ctx, final OneofDescriptor current,
                           final OneofDescriptor candidate) {
    adaptee.validateOneof(ctx, candidate);
  }

  @Override
  public void serviceAdded(final ValidationContext ctx, final ServiceDescriptor candidate) {
    adaptee.validateService(ctx, candidate);
  }

  @Override
  public void serviceRemoved(final ValidationContext ctx, final ServiceDescriptor current) {
  }

  @Override
  public void serviceChanged(final ValidationContext ctx, final ServiceDescriptor current,
                             final ServiceDescriptor candidate) {
    adaptee.validateService(ctx, candidate);
  }

  @Override
  public void methodAdded(final ValidationContext ctx, final MethodDescriptor candidate) {
    adaptee.validateMethod(ctx, candidate);
  }

  @Override
  public void methodRemoved(final ValidationContext ctx, final MethodDescriptor current) {
  }

  @Override
  public void methodChanged(final ValidationContext ctx, final MethodDescriptor current,
                            final MethodDescriptor candidate) {
    adaptee.validateMethod(ctx, candidate);
  }

  @Override
  public void fileAdded(final ValidationContext ctx, final FileDescriptor candidate) {
    adaptee.validateFile(ctx, candidate);
  }

  @Override
  public void fileRemoved(final ValidationContext ctx, final FileDescriptor current) {
  }

  @Override
  public void fileChanged(final ValidationContext ctx, final FileDescriptor current,
                          final FileDescriptor candidate) {
    adaptee.validateFile(ctx, candidate);
  }
}
