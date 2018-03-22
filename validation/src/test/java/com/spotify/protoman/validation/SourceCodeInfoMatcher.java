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

package com.spotify.protoman.validation;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.SourceCodeInfo;
import org.hamcrest.Matcher;

public class SourceCodeInfoMatcher
    extends MatcherHelperBase<SourceCodeInfo, SourceCodeInfoMatcher> {

  private SourceCodeInfoMatcher(final ImmutableList<Matcher<SourceCodeInfo>> matchers) {
    super(SourceCodeInfo.class, matchers, SourceCodeInfoMatcher::new);
  }

  public static SourceCodeInfoMatcher sourceCodeInfo() {
    return new SourceCodeInfoMatcher(ImmutableList.of());
  }

  public SourceCodeInfoMatcher start(final Matcher<SourceCodeInfo.FilePosition> matcher) {
    return where(SourceCodeInfo::start, "start", matcher);
  }

  public SourceCodeInfoMatcher end(final Matcher<SourceCodeInfo.FilePosition> matcher) {
    return where(SourceCodeInfo::end, "end", matcher);
  }

  public SourceCodeInfoMatcher leadingComments(final Matcher<String> matcher) {
    return where(SourceCodeInfo::leadingComments, "end", matcher);
  }
}
