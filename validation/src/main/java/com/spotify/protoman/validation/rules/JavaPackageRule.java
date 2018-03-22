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

import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.validation.ComparingValidationRule;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

public class JavaPackageRule implements ComparingValidationRule {

  private JavaPackageRule() {
  }

  public static JavaPackageRule create() {
    return new JavaPackageRule();
  }

  @Override
  public void fileAdded(final ValidationContext ctx, final FileDescriptor candidate) {
    validateJavaPackageOptionSet(ctx, candidate);
  }

  @Override
  public void fileChanged(
      final ValidationContext ctx, final FileDescriptor current, final FileDescriptor candidate) {
    validateJavaPackageOptionSet(ctx, candidate);

    if (!Objects.equals(current.javaPackage(), candidate.javaPackage())) {
      ctx.report(
          ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
          "Java package changed"
      );
    }
  }

  private void validateJavaPackageOptionSet(final ValidationContext ctx, final FileDescriptor descriptor) {
    if (!descriptor.options().hasJavaPackage()) {
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "java_package option should be set"
      );
    }
  }
}
