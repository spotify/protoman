package com.spotify.protoman.validation;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.GenericDescriptor;
import org.hamcrest.Matcher;

public class ValidationViolationMatcher
    extends MatcherHelperBase<ValidationViolation, ValidationViolationMatcher> {

  private ValidationViolationMatcher(final ImmutableList<Matcher<ValidationViolation>> matchers) {
    super(ValidationViolation.class, matchers, ValidationViolationMatcher::new);
  }

  public static ValidationViolationMatcher validationViolation() {
    return new ValidationViolationMatcher(ImmutableList.of());
  }

  public ValidationViolationMatcher type(final Matcher<ViolationType> matcher) {
    return where(ValidationViolation::type, "type", matcher);
  }

  public ValidationViolationMatcher current(final Matcher<GenericDescriptor> matcher) {
    return where(ValidationViolation::current, "current", matcher);
  }

  public ValidationViolationMatcher candidate(final Matcher<GenericDescriptor> matcher) {
    return where(ValidationViolation::candidate, "candidate", matcher);
  }

  public ValidationViolationMatcher description(final Matcher<String> matcher) {
    return where(ValidationViolation::description, "description", matcher);
  }
}
