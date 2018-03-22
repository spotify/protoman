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

package com.spotify.protoman.validation.rules;

import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;
import java.util.Optional;

public class MethodInputTypeCompatibilityRule implements ComparingValidationRule {

  private final TypeCompatibilityChecker checker;

  @FunctionalInterface
  interface TypeCompatibilityChecker {
    Optional<TypeCompatibility.TypeIncompatibility> checkTypeCompatibility(
        MessageDescriptor current, MessageDescriptor candidate);
  }

  private MethodInputTypeCompatibilityRule(final TypeCompatibilityChecker checker) {
    this.checker = checker;
  }

  public static MethodInputTypeCompatibilityRule create() {
    return create(TypeCompatibility::checkMessageTypeCompatibility);
  }

  static MethodInputTypeCompatibilityRule create(final TypeCompatibilityChecker checker) {
    return new MethodInputTypeCompatibilityRule(checker);
  }

  @Override
  public void methodChanged(final ValidationContext ctx,
                            final MethodDescriptor current,
                            final MethodDescriptor candidate) {
    final Optional<TypeCompatibility.TypeIncompatibility> incompatibility =
        checker.checkTypeCompatibility(current.inputType(), candidate.inputType());

    if (incompatibility.isPresent()) {
      incompatibility.ifPresent(typeIncompatibility -> ctx.report(
            typeIncompatibility.type(),
            "input type changed: " + typeIncompatibility.description()
      ));
    } else if (!Objects.equals(current.inputType().fullName(), candidate.inputType().fullName())) {
      // Changing the input type will change the generated source code
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "method input type changed"
      );
    }
  }
}
