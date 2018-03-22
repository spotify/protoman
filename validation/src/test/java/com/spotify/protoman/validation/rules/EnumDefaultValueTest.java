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
public class EnumDefaultValueTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(EnumDefaultValueRule.create())
      .build();

  private static final String TEMPLATE =
      "syntax = 'proto3';\n"
      + "enum AnEnum {\n"
      + "  %s = 0;\n"
      + "  ANOTHER_VALUE = 1;\n"
      + "}";

  private static Object[] allowedNames() {
    return new Object[]{
        "AN_ENUM_UNSPECIFIED",
        "AN_ENUM_UNKNOWN",
        "UNSPECIFIED",
        "UNKNOWN",
        "UNSPECIFIED_X",
        "UNKNOWN_X",
        "UNKNOWNX",
        "UNSPECIFIEDX",
    };
  }

  private static Object[] disallowedNames() {
    return new Object[]{
        "HERPADERP",
        "MY_DEFAULT",
    };
  }

  @Parameters(method = "allowedNames")
  @Test
  public void testAllowedName(final String name) throws Exception {
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, name)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(DescriptorSet.empty(), candidate);

    assertThat(violations, is(empty()));
  }

  @Parameters(method = "disallowedNames")
  @Test
  public void testDisallowedName(final String name) throws Exception {
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, name)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(DescriptorSet.empty(), candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo(
                    "enum value 0 should be used for unknown value, e.g. AN_ENUM_UNSPECIFIED"
                ))
                .type(equalTo(ViolationType.BEST_PRACTICE_VIOLATION))
                .current(nullValue(GenericDescriptor.class))
                .candidate(genericDescriptor()
                    .sourceCodeInfo(optionalWithValue(sourceCodeInfo()
                    .start(
                        filePosition()
                            .line(2)
                            .column(1)
                    ))))
        )
    );
  }
}