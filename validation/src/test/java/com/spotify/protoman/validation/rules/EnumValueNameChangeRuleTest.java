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

public class EnumValueNameChangeRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(EnumValueNameChangeRule.create())
      .build();

  // TODO(staffan): Add tests for when "option allow_alias = true;" is used

  @Test
  public void testEnumValueNameUnchanged() throws Exception {
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "enum Florb {\n"
        + "  FLORB_UNKNOWN = 0;\n"
        + "  FLORB_A = 1;\n"
        + "}"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(candidate, candidate);

    assertThat(violations, is(empty()));
  }

  @Test
  public void testEnumValueNameChanged() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "enum Florb {\n"
        + "  FLORB_UNKNOWN = 0;\n"
        + "  FLORB_A = 1;\n"
        + "}"
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "enum Florb {\n"
        + "  FLORB_UNKNOWN = 0;\n"
        + "  FLORB_B = 1;\n"
        + "}"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION))
                .description(equalTo("enum value name changed"))
        )
    );
  }
}