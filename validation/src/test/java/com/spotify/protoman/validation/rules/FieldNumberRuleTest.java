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
import org.junit.Test;

public class FieldNumberRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(FieldNumberRule.create())
      .build();

  private static final String TEMPLATE =
      "syntax = 'proto3';\n"
      + "message Derp {\n"
      + "  int32 a_field = %d;\n"
      + "}";

  @Test
  public void testFieldNumberChanged() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, 1)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, 2)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.WIRE_INCOMPATIBILITY_VIOLATION))
                .description(equalTo("field number changed"))
        )
    );
  }

  @Test
  public void testFieldNumberNotChanged() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, 1)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, 1)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, is(empty()));
  }
}