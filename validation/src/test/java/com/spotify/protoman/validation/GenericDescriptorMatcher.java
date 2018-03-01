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
