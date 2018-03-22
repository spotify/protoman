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

import com.spotify.protoman.descriptor.FieldDescriptor;
import com.spotify.protoman.descriptor.MessageDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class FieldRemovalRule implements ComparingValidationRule {

  private FieldRemovalRule() {
  }

  public static FieldRemovalRule create() {
    return new FieldRemovalRule();
  }

  @Override
  public void fieldRemoved(final ValidationContext ctx,
                           final FieldDescriptor current,
                           final MessageDescriptor candidateContainingMessage) {
    if (candidateContainingMessage.isReservedNumber(current.number())
        && candidateContainingMessage.isReservedName(current.name())) {
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "field made reserved"
      );
    } else {
      // TODO(staffan): Removing a field is tehnically okay, but we probably shouldn't encourage
      // it, as bad things will happen if the same field number is reused later. Warn at more
      // severe level?
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "field removed"
      );
    }
  }
}
