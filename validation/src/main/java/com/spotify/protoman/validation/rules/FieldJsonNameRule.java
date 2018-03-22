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
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

public class FieldJsonNameRule implements ComparingValidationRule {

  private FieldJsonNameRule() {
  }

  public static FieldJsonNameRule create() {
    return new FieldJsonNameRule();
  }

  @Override
  public void fieldChanged(final ValidationContext ctx,
                           final FieldDescriptor current,
                           final FieldDescriptor candidate) {
    if (!Objects.equals(candidate.jsonName(), current.jsonName())) {
      // TODO(staffan): Should we use different levels to indicate breaking binary, json and text
      // encoding? (the latter we don't warn for at all currently)
      ctx.report(
          ViolationType.JSON_ENCODING_INCOMPATIBILITY,
          String.format("json name changed (%s -> %s)", current.jsonName(), candidate.jsonName())
      );
    }
  }
}
