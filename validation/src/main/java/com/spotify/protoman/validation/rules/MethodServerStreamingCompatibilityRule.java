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

public class MethodServerStreamingCompatibilityRule implements ComparingValidationRule {

  private MethodServerStreamingCompatibilityRule() {
  }

  public static MethodServerStreamingCompatibilityRule create() {
    return new MethodServerStreamingCompatibilityRule();
  }

  @Override
  public void methodChanged(final ValidationContext ctx,
                            final MethodDescriptor current,
                            final MethodDescriptor candidate) {
    // TODO(staffan): is this wire compatible?
    if (current.toProto().getServerStreaming() != candidate.toProto().getServerStreaming()) {
      ctx.report(
          ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
          "changed to/from server streaming"
      );
    }
  }
}
