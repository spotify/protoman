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
import com.spotify.protoman.descriptor.FileDescriptor;
import com.spotify.protoman.descriptor.GenericDescriptor;
import com.spotify.protoman.descriptor.SourceCodeInfo;
import java.util.Optional;
import org.hamcrest.Matcher;

public class GenericDescriptorMatcher
    extends MatcherHelperBase<GenericDescriptor, GenericDescriptorMatcher> {

  private GenericDescriptorMatcher(final ImmutableList<Matcher<GenericDescriptor>> matchers) {
    super(GenericDescriptor.class, matchers, GenericDescriptorMatcher::new);
  }

  public static GenericDescriptorMatcher genericDescriptor() {
    return new GenericDescriptorMatcher(ImmutableList.of());
  }

  public GenericDescriptorMatcher sourceCodeInfo(
      final Matcher<Optional<? extends SourceCodeInfo>> matcher) {
    return where(GenericDescriptor::sourceCodeInfo, "sourceCodeInfo", matcher);
  }

  public GenericDescriptorMatcher fullName(final Matcher<String> matcher) {
    return where(GenericDescriptor::fullName, "fullName", matcher);
  }

  public GenericDescriptorMatcher name(final Matcher<String> matcher) {
    return where(GenericDescriptor::name, "name", matcher);
  }

  public GenericDescriptorMatcher file(final Matcher<FileDescriptor> matcher) {
    return where(GenericDescriptor::file, "file", matcher);
  }
}
