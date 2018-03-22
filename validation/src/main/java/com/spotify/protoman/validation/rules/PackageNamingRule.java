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
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;
import java.util.Objects;

public class PackageNamingRule implements ValidationRule {

  private PackageNamingRule() {
  }

  public static PackageNamingRule create() {
    return new PackageNamingRule();
  }

  @Override
  public void validateFile(final ValidationContext ctx, final FileDescriptor candidate) {
    // Package name should be all lower-case
    if (!Objects.equals(candidate.protoPackage().toLowerCase(), candidate.protoPackage())) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "package name should be all-lower case"
      );
    }

    // Package name should not contain "schema" or "proto"
    final String lowerCasePackage = candidate.protoPackage().toLowerCase();
    if (lowerCasePackage.endsWith(".proto") || lowerCasePackage.contains(".proto.")
        || lowerCasePackage.endsWith(".schema") || lowerCasePackage.contains(".schema.")) {
      ctx.report(
          ViolationType.STYLE_GUIDE_VIOLATION,
          "package name should not contain 'schema' or 'proto'"
      );
    }
  }
}
