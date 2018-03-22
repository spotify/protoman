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

import com.spotify.protoman.descriptor.ServiceDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;

public class ServiceRemovalRule implements ComparingValidationRule {

  private ServiceRemovalRule() {
  }

  public static ServiceRemovalRule create() {
    return new ServiceRemovalRule();
  }

  @Override
  public void serviceRemoved(final ValidationContext ctx, final ServiceDescriptor current) {
    if (!current.options().getDeprecated()) {
      // TODO(staffan): is this "wire-incompatible"?!?!
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "service removed"
      );
    }
  }
}
