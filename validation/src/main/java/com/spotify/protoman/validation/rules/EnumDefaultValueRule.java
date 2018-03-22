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

import com.google.common.base.CaseFormat;
import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;

public class EnumDefaultValueRule implements ValidationRule {

  private EnumDefaultValueRule() {
  }

  public static EnumDefaultValueRule create() {
    return new EnumDefaultValueRule();
  }

  @Override
  public void validateEnum(final ValidationContext ctx, final EnumDescriptor candidate) {
    // If "option allow_alias = true;" is used there can be multiple values which number 0.
    // Find values with number 0 that contains UNKNOWN or UNSPECIFIED and warn if there are none.
    if (candidate.findValuesByNumber(0)
            .filter(value -> {
              final String lowerCaseName = value.name().toUpperCase();
              return lowerCaseName.contains("UNKNOWN") || lowerCaseName.contains("UNSPECIFIED");
            }).count() < 1) {
      final String suggestedName =
          CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, candidate.name())
          + "_UNSPECIFIED";
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "enum value 0 should be used for unknown value, e.g. " + suggestedName
      );
    }
  }
}
