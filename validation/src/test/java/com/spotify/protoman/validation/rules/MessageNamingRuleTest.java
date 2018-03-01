package com.spotify.protoman.validation.rules;

import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static com.spotify.protoman.validation.FilePositionMatcher.filePosition;
import static com.spotify.protoman.validation.GenericDescriptorMatcher.genericDescriptor;
import static com.spotify.protoman.validation.SourceCodeInfoMatcher.sourceCodeInfo;
import static com.spotify.protoman.validation.ValidationViolationMatcher.validationViolation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.descriptor.GenericDescriptor;
import com.spotify.protoman.testutil.DescriptorSetUtils;
import com.spotify.protoman.validation.DefaultSchemaValidator;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import com.spotify.protoman.validation.ViolationType;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class MessageNamingRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(MessageNamingRule.create())
      .build();

  private static final String TEMPLATE =
      "syntax = 'proto3';\n"
      + "message %s {\n"
      + "}";

  private static Object[] allowedNames() {
    return new Object[]{
        "UpperCamelCase",
        "XPath",
        "X",
    };
  }

  private static Object[] disallowedNames() {
    return new Object[]{
        "UPPER_SNAKE_CASE",
        "lowerCamelCase",
        "lower_snake_case",
        "UPPER_SNAKE_CASE",
        "MIXED_snake_Case",
        "Snake_Camel_Case",
        "lowercase",
        "UPPERCASE",
    };
  }

  @Parameters(method = "allowedNames")
  @Test
  public void testAllowedName_new(final String name) throws Exception {
    final DescriptorSet current = DescriptorSet.empty();
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, name)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, is(empty()));
  }

  @Parameters(method = "allowedNames")
  @Test
  public void testAllowedName_existing(final String name) throws Exception {
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, name)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(candidate, candidate);

    assertThat(violations, is(empty()));
  }

  @Parameters(method = "disallowedNames")
  @Test
  public void testDisallowedName_new(final String name) throws Exception {
    final DescriptorSet current = DescriptorSet.empty();
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, name)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo("message name should be UpperCamelCase"))
                .type(equalTo(ViolationType.STYLE_GUIDE_VIOLATION))
                .current(nullValue(GenericDescriptor.class))
                .candidate(genericDescriptor()
                    .sourceCodeInfo(optionalWithValue(
                        sourceCodeInfo().start(
                            filePosition()
                                .line(2)
                                .column(1)
                        )))
                )
        )
    );
  }

  @Parameters(method = "disallowedNames")
  @Test
  public void testDisallowedName_existing(final String name) throws Exception {
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, name)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(candidate, candidate);

    assertThat(violations, is(empty()));
  }
}