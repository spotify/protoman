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

import com.spotify.protoman.descriptor.MethodDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class MethodRemovalRule implements ComparingValidationRule {

  private MethodRemovalRule() {
  }

  public static MethodRemovalRule create() {
    return new MethodRemovalRule();
  }

  @Override
  public void methodRemoved(final ValidationContext ctx, final MethodDescriptor current) {
    // TODO(staffan): Take things like lifecycle into account here. Removing methods from a GA
    // service is a no-no but removing it from a experimental service is probably okay.
    // Would be very cool if we could get the number of users of a service and base decision off of
    // that.
    if (!current.options().getDeprecated()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "method removed"
      );
    }
  }
}
