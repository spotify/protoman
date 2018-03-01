package com.spotify.protoman.validation;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.StringDescription;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * Helper class to implement hamcrest matchers for "complex" objects. Matchers built using this
 * class behave similarly to IsPojo from hamcrest-pojo but are strongly typed.
 *
 * Example of how you'd use the resulting matchers:
 *
 * {@code
 * assertThat(
 *   violations,
 *   contains(
 *     validationViolation()
 *       .description(equalTo("field name should be lower_snake_case"))
 *       .type(equalTo(ValidationType.STYLE_GUIDE_VIOLATION))
 *       ...
 *   )
 * );
 * }
 *
 * Example output:
 *
 * {@code
 * Expected: iterable containing [ValidationViolation {
 *   description: "field name should be lower_snake_case"
 *   type: <STYLE_GUIDE_VIOLATION>
 *   current: null
 *   candidate: GenericDescriptor {
 *     sourceCodeInfo: an Optional with a value that SourceCodeInfo {
 *       start: FilePosition {
 *         line: <4>
 *         column: <4>
 *       }
 *     }
 *   }
 * }]
 *      but: item 0: ValidationViolation {
 *     candidate: GenericDescriptor {
 *       sourceCodeInfo: was an Optional whose value SourceCodeInfo {
 *         start: FilePosition {
 *           line: was <3>
 *           column: was <3>
 *         }
 *       }
 *     }
 *   }
 * }
 *
 * @param <T> Type being matched.
 * @param <MatcherType> Matcher subtype (i.e. type of the inheriting matcher).
 */
public class MatcherHelperBase<T, MatcherType> extends TypeSafeDiagnosingMatcher<T> {

  private final ImmutableList<Matcher<T>> matchers;
  private final String className;
  private final Function<ImmutableList<Matcher<T>>, MatcherType> factory;

  protected MatcherHelperBase(final Class<T> clazz, final ImmutableList<Matcher<T>> matchers,
                              final Function<ImmutableList<Matcher<T>>, MatcherType> factory) {
    this.matchers = matchers;
    this.className = clazz.getSimpleName();
    this.factory = factory;
  }

  @Override
  protected boolean matchesSafely(final T item, final Description mismatchDescription) {
    boolean ret = true;
    mismatchDescription.appendText(className).appendText(" {\n");
    for (final Matcher<T> matcher : matchers) {
      if (!matcher.matches(item)) {
        mismatchDescription.appendText("  ");
        Description innerDescription = new StringDescription();
        matcher.describeMismatch(item, innerDescription);
        indentDescription(mismatchDescription, innerDescription);
        ret = false;
      }
    }
    mismatchDescription.appendText("}");
    return ret;
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText(className).appendText(" {\n");
    for (final Matcher<T> matcher : matchers) {
      description.appendText("  ");
      Description innerDescription = new StringDescription();
      matcher.describeTo(innerDescription);
      indentDescription(description, innerDescription);
    }
    description.appendText("}");
  }

  private void indentDescription(Description description, Description innerDescription) {
    description
        .appendText(
            Joiner.on("\n  ").join(Splitter.on('\n').split(innerDescription.toString())))
        .appendText("\n");
  }

  protected <R> MatcherType where(final Function<T, R> mapper,
                                  final String name,
                                  final Matcher<R> matcher) {
    final ImmutableList.Builder<Matcher<T>> builder =
        ImmutableList.<Matcher<T>>builder()
            .addAll(matchers)
            .add(new FeatureMatcher<T , R>(matcher, name + ":", name + ":") {
              @Override
              protected R featureValueOf(final T actual) {
                return mapper.apply(actual);
              }
            });
    return factory.apply(builder.build());
  }
}
