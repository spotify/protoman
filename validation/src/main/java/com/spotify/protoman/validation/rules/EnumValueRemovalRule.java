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

import com.spotify.protoman.descriptor.EnumDescriptor;
import com.spotify.protoman.descriptor.EnumValueDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class EnumValueRemovalRule implements ComparingValidationRule {

  private EnumValueRemovalRule() {
  }

  public static EnumValueRemovalRule create() {
    return new EnumValueRemovalRule();
  }

  @Override
  public void enumValueRemoved(final ValidationContext ctx, final EnumValueDescriptor current,
                               final EnumDescriptor candidateContainingEnum) {
    if (candidateContainingEnum.isReservedNumber(current.number())
        && candidateContainingEnum.isReservedName(current.name())) {
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "enum value made reserved"
      );
    } else {
      // TODO(staffan): Removing a field is tehnically okay, but we probably shouldn't encourage
      // it, as bad things will happen if the same field number is reused later. Warn at more
      // severe level?
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "enum value removed"
      );
    }
  }
}
