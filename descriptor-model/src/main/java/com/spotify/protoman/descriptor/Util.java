/*-
 * -\-\-
 * protoman-descriptor-model
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

package com.spotify.protoman.descriptor;

import javax.annotation.Nullable;

class Util {

  private Util() {
  }

  static String computeFullName(final FileDescriptor file,
                                @Nullable final MessageDescriptor parent,
                                final String name) {
    if (parent != null) {
      return parent.fullName() + '.' + name;
    } else if (file.protoPackage().length() > 0) {
      return file.protoPackage() + '.' + name;
    } else {
      return name;
    }
  }
}
