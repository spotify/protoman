package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.descriptor.OneofDescriptor;
import com.spotify.protoman.descriptor.ServiceDescriptor;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

/**
 * Enforces naming conventions are followed for fields, services, method, etc.
 */
public class NamingRule implements ValidationRule {

  private NamingRule() {
  }

  public static NamingRule create() {
    return new NamingRule();
  }

  @Override
  public void messageAdded(final Context ctx, final MessageDescriptor candidate) {
    if (!isUpperCamelCaseName(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "message name should be UpperCamelCase"
      );
    }
  }

  @Override
  public void fieldAdded(final Context ctx,
                         final FieldDescriptor candidate,
                         final MessageDescriptor candidateContainingMessage) {
    if (!isLowerSnakeCase(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "field name should be lower_snake_case"
      );
    }
  }

  @Override
  public void enumAdded(final Context ctx, final EnumDescriptor candidate) {
    if (!isUpperCamelCaseName(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "enum name should be UpperCamelCase"
      );
    }
  }

  @Override
  public void enumValueAdded(final Context ctx, final EnumValueDescriptor candidate) {
    if (!isUpperSnakeCase(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "enum value should be UPPER_SNAKE_CASE"
      );
    }
  }

  @Override
  public void serviceAdded(final Context ctx, final ServiceDescriptor candidate) {
    if (!isUpperCamelCaseName(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "service names should be UpperCamelCase"
      );
    }
  }

  @Override
  public void methodAdded(final Context ctx, final MethodDescriptor candidate) {
    if (!isUpperCamelCaseName(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "method names should be UpperCamelCase"
      );
    }
  }

  @Override
  public void oneofAdded(final Context ctx, final OneofDescriptor candidate) {
    if (!isLowerSnakeCase(candidate.name())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "oneof name should be lower_snake_case"
      );
    }
  }

  private static boolean isLowerSnakeCase(final String name) {
    return Objects.equals(name.toLowerCase(), name);
  }

  private static boolean isUpperCamelCaseName(final String name) {
    return Character.isUpperCase(name.charAt(0)) && !name.contains("_");
  }

  // For checking enum value names
  private static boolean isUpperSnakeCase(final String name) {
    return Objects.equals(name.toUpperCase(), name);
  }
}
