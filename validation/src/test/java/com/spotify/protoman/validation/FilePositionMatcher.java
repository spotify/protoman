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

import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.SourceCodeInfo;
import org.hamcrest.Matcher;

public class FilePositionMatcher
    extends MatcherHelperBase<SourceCodeInfo.FilePosition, FilePositionMatcher> {

  private FilePositionMatcher(final ImmutableList<Matcher<SourceCodeInfo.FilePosition>> matchers) {
    super(SourceCodeInfo.FilePosition.class, matchers, FilePositionMatcher::new);
  }

  public static FilePositionMatcher filePosition() {
    return new FilePositionMatcher(ImmutableList.of());
  }

  public FilePositionMatcher line(final Matcher<Integer> matcher) {
    return where(SourceCodeInfo.FilePosition::line, "line", matcher);
  }

  public FilePositionMatcher line(final int line) {
    return line(equalTo(line));
  }

  public FilePositionMatcher column(final Matcher<Integer> matcher) {
    return where(SourceCodeInfo.FilePosition::column, "column", matcher);
  }

  public FilePositionMatcher column(final int column) {
    return column(equalTo(column));
  }
}
