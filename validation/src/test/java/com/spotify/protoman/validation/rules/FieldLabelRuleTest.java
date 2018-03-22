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

import static com.spotify.protoman.validation.ValidationViolationMatcher.validationViolation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.DescriptorSet;
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
public class FieldLabelRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(FieldLabelRule.create())
      .build();

  private static Object[] wireIncompat() {
    return new Object[]{
        // from, to
        new Object[]{"required", "optional"},
        new Object[]{"required", "repeated"},
        new Object[]{"repeated", "required"},
        new Object[]{"optional", "required"}
    };
  }

  private static Object[] generatedSourceIncompat() {
    return new Object[]{
        // from, to
        new Object[]{"repeated", "optional"},
        new Object[]{"optional", "repeated"},
    };
  }

  private static Object[] fieldLabels() {
    return new Object[]{
        "optional", "repeated", "required"
    };
  }

  @Parameters(method = "wireIncompat")
  @Test
  public void testWireIncompatChange(final String from, final String to) throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        String.format(
            "syntax = 'proto2';\n"
            + "message AMessage {\n"
            + "  %s int32 a_field = 1;"
            + "}", from)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        String.format(
            "syntax = 'proto2';\n"
            + "message AMessage {\n"
            + "  %s int32 a_field = 1;"
            + "}", to)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.WIRE_INCOMPATIBILITY_VIOLATION))
                .description(equalTo("field label changed to/from required"))
        )
    );
  }

  @Parameters(method = "generatedSourceIncompat")
  @Test
  public void testGeneratedSourceIncompat(final String from, final String to) throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        String.format(
            "syntax = 'proto2';\n"
            + "message AMessage {\n"
            + "  %s int32 a_field = 1;"
            + "}", from)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        String.format(
            "syntax = 'proto2';\n"
            + "message AMessage {\n"
            + "  %s int32 a_field = 1;"
            + "}", to)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION))
                .description(equalTo("field label changed"))
        )
    );
  }

  @Parameters(method = "fieldLabels")
  @Test
  public void testLabelNotChanged(final String label) throws Exception {
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        String.format(
            "syntax = 'proto2';\n"
            + "message AMessage {\n"
            + "  %s int32 a_field = 1;"
            + "}", label)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(candidate, candidate);

    assertThat(violations, is(empty()));
  }
}