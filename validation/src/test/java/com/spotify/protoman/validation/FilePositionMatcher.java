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
