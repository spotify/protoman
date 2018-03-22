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
public class OneofNamingRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(OneofNamingRule.create())
      .build();

  private static final String TEMPLATE =
      "syntax = 'proto3';\n"
      + "message Derp {\n"
      + "  oneof %s {\n"
      + "    int32 one = 1;\n"
      + "    int32 two = 2;\n"
      + "  }\n"
      + "}";

  private static Object[] allowedNames() {
    return new Object[]{
        "lower_snake_case",
        "lowercase",
        "x",
    };
  }

  private static Object[] disallowedNames() {
    return new Object[]{
        "lowerCamelCase",
        "UpperCamelCase",
        "UPPER_SNAKE_CASE",
        "MIXED_snake_Case",
        "UPPERCASE",
        "Snake_Camel_Case",
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
                .description(equalTo("oneof name should be lower_snake_case"))
                .type(equalTo(ViolationType.STYLE_GUIDE_VIOLATION))
                .current(nullValue(GenericDescriptor.class))
                .candidate(genericDescriptor()
                    .sourceCodeInfo(optionalWithValue(
                        sourceCodeInfo().start(
                            filePosition()
                                .line(3)
                                .column(3)
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