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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.descriptor.GenericDescriptor;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.descriptor.OneofDescriptor;
import com.spotify.protoman.descriptor.ServiceDescriptor;
import com.spotify.protoman.validation.rules.EnumDefaultValueRule;
import com.spotify.protoman.validation.rules.EnumNamingRule;
import com.spotify.protoman.validation.rules.EnumValueNameChangeRule;
import com.spotify.protoman.validation.rules.EnumValueNamingRule;
import com.spotify.protoman.validation.rules.EnumValueRemovalRule;
import com.spotify.protoman.validation.rules.FieldJsonNameRule;
import com.spotify.protoman.validation.rules.FieldLabelRule;
import com.spotify.protoman.validation.rules.FieldNameChangeRule;
import com.spotify.protoman.validation.rules.FieldNamingRule;
import com.spotify.protoman.validation.rules.FieldRemovalRule;
import com.spotify.protoman.validation.rules.FieldTypeCompatibilityRule;
import com.spotify.protoman.validation.rules.FilePathAndPackageMatchRule;
import com.spotify.protoman.validation.rules.MessageNamingRule;
import com.spotify.protoman.validation.rules.MethodClientStreamingCompatibilityRule;
import com.spotify.protoman.validation.rules.MethodIdempotencyChangeRule;
import com.spotify.protoman.validation.rules.MethodInputTypeCompatibilityRule;
import com.spotify.protoman.validation.rules.MethodNamingRule;
import com.spotify.protoman.validation.rules.MethodOutputTypeCompatibilityRule;
import com.spotify.protoman.validation.rules.MethodRemovalRule;
import com.spotify.protoman.validation.rules.MethodServerStreamingCompatibilityRule;
import com.spotify.protoman.validation.rules.OneofNamingRule;
import com.spotify.protoman.validation.rules.PackageNamingRule;
import com.spotify.protoman.validation.rules.PackageRequiredRule;
import com.spotify.protoman.validation.rules.ServiceNamingRule;
import com.spotify.protoman.validation.rules.ServiceRemovalRule;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class DefaultSchemaValidator implements SchemaValidator {

  private final ImmutableList<ComparingValidationRule> rules;

  private DefaultSchemaValidator(final Stream<ComparingValidationRule> ruleStream) {
    this.rules = ruleStream.collect(ImmutableList.toImmutableList());
  }

  public static DefaultSchemaValidator create(final Stream<ComparingValidationRule> ruleStream) {
    return new DefaultSchemaValidator(ruleStream);
  }

  public static DefaultSchemaValidator withDefaultRules() {
    return builder()
        .addDefaultRules()
        .build();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public ImmutableList<ValidationViolation> validate(final DescriptorSet current,
                                                     final DescriptorSet candidate) {
    final ImmutableList.Builder<ValidationViolation> violations = ImmutableList.builder();
    final ContextImpl ctx = new ContextImpl(violations);
    final ValidationVisitor visitor = new ValidationVisitor(ctx);
    DescriptorSet.compare(visitor, current, candidate);
    return violations.build();
  }

  public static class Builder {

    private final List<ComparingValidationRule> rules = new ArrayList<>();

    private Builder() {
    }

    public Builder addDefaultRules() {
      addRule(EnumDefaultValueRule.create());
      addRule(EnumValueNameChangeRule.create());
      addRule(FieldJsonNameRule.create());
      addRule(FieldLabelRule.create());
      addRule(FieldTypeCompatibilityRule.create());
      addRule(MethodServerStreamingCompatibilityRule.create());
      addRule(MethodClientStreamingCompatibilityRule.create());
      addRule(MethodInputTypeCompatibilityRule.create());
      addRule(MethodOutputTypeCompatibilityRule.create());
      addRule(MethodIdempotencyChangeRule.create());
      addRule(FieldNameChangeRule.create());
      // Naming rules
      addRule(MessageNamingRule.create());
      addRule(FieldNamingRule.create());
      addRule(OneofNamingRule.create());
      addRule(EnumNamingRule.create());
      addRule(EnumValueNamingRule.create());
      addRule(ServiceNamingRule.create());
      addRule(MethodNamingRule.create());
      addRule(PackageNamingRule.create());
      // Package best-pratices
      addRule(PackageRequiredRule.create());
      addRule(FilePathAndPackageMatchRule.create());
      // Rules governing removal of types
      addRule(FieldRemovalRule.create());
      addRule(EnumValueRemovalRule.create());
      addRule(ServiceRemovalRule.create());
      addRule(MethodRemovalRule.create());
      return this;
    }

    public Builder addRule(final ComparingValidationRule rule) {
      rules.add(rule);
      return this;
    }

    public Builder addRule(final ValidationRule rule) {
      rules.add(RuleAdapter.adapt(rule));
      return this;
    }

    public DefaultSchemaValidator build() {
      return create(rules.stream());
    }
  }

  private class ValidationVisitor implements DescriptorSet.ComparingVisitor {

    private final ContextImpl ctx;

    private ValidationVisitor(final ContextImpl ctx) {
      this.ctx = ctx;
    }

    /**
     * Helper for choosing the right callback to call depending on whether a "thing" was removed,
     * added or changed.
     */
    private <T extends GenericDescriptor> void dispatch(
        @Nullable final T current,
        @Nullable final T candidate,
        final AdditionCallback<T> added,
        final RemovalCallback<T> removed,
        final ChangeCallback<T> changed) {
      checkArgument(current != null || candidate != null);
      ctx.setDescriptors(current, candidate);
      if (candidate == null) {
        removed.accept(ctx, current);
      } else if (current == null) {
        added.accept(ctx, candidate);
      } else if (!current.toProto().equals(candidate.toProto())) {
        // Run change validation only if the descriptors differ
        changed.accept(ctx, current, candidate);
      }
    }

    @Override
    public void visit(@Nullable final FieldDescriptor current,
                      @Nullable final FieldDescriptor candidate,
                      @Nullable final MessageDescriptor currentContainingMessage,
                      @Nullable final MessageDescriptor candidateContainingMessage) {
      checkArgument(current != null || candidate != null);
      ctx.setDescriptors(current, candidate);
      if (candidate == null) {
        if (candidateContainingMessage != null) {
          rules.forEach(rule -> rule.fieldRemoved(
              ctx, current, checkNotNull(candidateContainingMessage)));
        }
      } else if (current == null) {
        rules.forEach(rule -> rule.fieldAdded(ctx, candidate));
      } else if (!current.toProto().equals(candidate.toProto())) {
        rules.forEach(rule -> rule.fieldChanged(ctx, current, candidate));
      }
    }

    @Override
    public void visit(@Nullable final MessageDescriptor current,
                      @Nullable final MessageDescriptor candidate) {
      rules.forEach(rule ->
          dispatch(current, candidate, rule::messageAdded,
              rule::messageRemoved, rule::messageChanged)
      );
    }

    @Override
    public void visit(@Nullable final EnumDescriptor current,
                      @Nullable final EnumDescriptor candidate) {
      rules.forEach(rule ->
          dispatch(current, candidate, rule::enumAdded,
              rule::enumRemoved, rule::enumChanged)
      );
    }

    @Override
    public void visit(@Nullable final EnumValueDescriptor current,
                      @Nullable final EnumValueDescriptor candidate,
                      @Nullable final EnumDescriptor currentContainingEnum,
                      @Nullable final EnumDescriptor candidateContainingEnum) {
      checkArgument(current != null || candidate != null);
      ctx.setDescriptors(current, candidate);
      if (candidate == null) {
        if (candidateContainingEnum != null) {
          rules.forEach(rule -> rule.enumValueRemoved(
              ctx, current, checkNotNull(candidateContainingEnum)));
        }
      } else if (current == null) {
        rules.forEach(rule -> rule.enumValueAdded(ctx, candidate));
      } else if (!current.toProto().equals(candidate.toProto())) {
        rules.forEach(rule -> rule.enumValueChanged(ctx, current, candidate));
      }
    }

    @Override
    public void visit(@Nullable final ServiceDescriptor current,
                      @Nullable final ServiceDescriptor candidate) {
      rules.forEach(rule ->
          dispatch(current, candidate, rule::serviceAdded,
              rule::serviceRemoved, rule::serviceChanged)
      );
    }

    @Override
    public void visit(@Nullable final MethodDescriptor current,
                      @Nullable final MethodDescriptor candidate) {
      rules.forEach(rule ->
          dispatch(current, candidate, rule::methodAdded,
              rule::methodRemoved, rule::methodChanged)
      );
    }

    @Override
    public void visit(@Nullable final OneofDescriptor current,
                      @Nullable final OneofDescriptor candidate) {
      rules.forEach(rule ->
          dispatch(current, candidate, rule::oneofAdded,
              rule::oneofRemoved, rule::oneofChanged)
      );
    }

    @Override
    public void visit(@Nullable final FileDescriptor current,
                      @Nullable final FileDescriptor candidate) {
      rules.forEach(rule ->
          dispatch(current, candidate, rule::fileAdded,
              rule::fileRemoved, rule::fileChanged)
      );
    }


    /*private void validateField(final Descriptors.FieldDescriptor current,
                               final Descriptors.FieldDescriptor candidate) {

      // ...

      if (fieldMovedOutOfOneof(current, candidate)) {
        violations.add(
            bestPracticeViolation("field moved out of oneof", candidate)
        );
      }

      if (fieldMovedIntoOneof(current, candidate)) {
        violations.add(
            bestPracticeViolation("field moved into oneof", candidate)
        );
      }

      if (fieldMovedToDifferentOneof(current, candidate)) {
        violations.add(
            bestPracticeViolation("field moved to different oneof", candidate)
        );
      }

      // TODO: Check http annotation. Default value?
    }*/



    /*private static boolean fieldMovedOutOfOneof(final Descriptors.FieldDescriptor prev,
                                                final Descriptors.FieldDescriptor next) {
      return prev.getContainingOneof() != null && next.getContainingOneof() == null;
    }

    private static boolean fieldMovedIntoOneof(final Descriptors.FieldDescriptor prev,
                                               final Descriptors.FieldDescriptor next) {
      return prev.getContainingOneof() == null && next.getContainingOneof() != null;
    }

    private static boolean fieldMovedToDifferentOneof(final Descriptors.FieldDescriptor prev,
                                                      final Descriptors.FieldDescriptor next) {
      return prev.getContainingOneof() != null
             && next.getContainingOneof() != null
             && !Objects.equals(
          prev.getContainingOneof().getName(), next.getContainingOneof().getName());
    }*/
  }

  @FunctionalInterface
  private interface AdditionCallback<T> {
    void accept(ValidationContext ctx, T candidate);
  }

  @FunctionalInterface
  private interface RemovalCallback<T> {
    void accept(ValidationContext ctx, T current);
  }

  @FunctionalInterface
  private interface ChangeCallback<T> {
    void accept(ValidationContext ctx, T current, T candidate);
  }

  private static class ContextImpl implements ValidationContext {

    private final ImmutableList.Builder<ValidationViolation> violations;
    @Nullable private GenericDescriptor current;
    @Nullable private GenericDescriptor candidate;

    private ContextImpl(final ImmutableList.Builder<ValidationViolation> violations) {
      this.violations = violations;
    }

    private void setDescriptors(@Nullable final GenericDescriptor current,
                                @Nullable final GenericDescriptor candidate) {
      checkArgument(current != null || candidate != null);
      this.current = current;
      this.candidate = candidate;
    }

    @Override
    public void report(final ViolationType type, final String description) {
      final ValidationViolation.Builder builder = ValidationViolation.builder()
          .setType(type)
          .setDescription(description);

      if (current != null) {
        builder.setCurrent(current);
      }

      if (candidate != null) {
        builder.setCandidate(candidate);
      }

      final ValidationViolation violation = builder.build();
      violations.add(violation);
    }
  }

}
