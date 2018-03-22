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

import java.util.Objects;

class CaseFormatUtil {

  private CaseFormatUtil() {
    // Prevent instantiation
  }

  static boolean isLowerSnakeCase(final String name) {
    return Objects.equals(name.toLowerCase(), name);
  }

  static boolean isUpperCamelCaseName(final String name) {
    return Character.isUpperCase(name.charAt(0))
           && !name.contains("_")
           && (name.length() == 1 || !Objects.equals(name, name.toUpperCase()));
  }

  // For checking enum value names
  static boolean isUpperSnakeCase(final String name) {
    return Objects.equals(name.toUpperCase(), name);
  }
}
