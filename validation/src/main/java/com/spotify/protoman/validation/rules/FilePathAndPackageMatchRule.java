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

import com.google.common.base.Joiner;
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.validation.ValidationContext;
import com.spotify.protoman.validation.ValidationRule;
import com.spotify.protoman.validation.ViolationType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class FilePathAndPackageMatchRule implements ValidationRule {

  private FilePathAndPackageMatchRule() {
  }

  public static FilePathAndPackageMatchRule create() {
    return new FilePathAndPackageMatchRule();
  }

  @Override
  public void validateFile(final ValidationContext ctx, final FileDescriptor candidate) {
    // Package name should match file path
    if (!filePathMatchesPackage(candidate)) {
      ctx.report(
          ViolationType.BEST_PRACTICE_VIOLATION,
          "proto file path must match package name"
      );
    }
  }

  private boolean filePathMatchesPackage(final FileDescriptor descriptor) {
    final Path parent = Paths.get(descriptor.file().name()).getParent();
    return parent != null && Objects.equals(Joiner.on(".").join(parent), descriptor.protoPackage());
  }
}
