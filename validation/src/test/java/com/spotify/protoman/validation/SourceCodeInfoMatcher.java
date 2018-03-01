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
