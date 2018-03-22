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
