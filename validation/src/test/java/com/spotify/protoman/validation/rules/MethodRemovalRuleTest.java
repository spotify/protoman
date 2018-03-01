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

public class MethodRemovalRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(MethodRemovalRule.create())
      .build();

  @Test
  public void testMethodRemoved() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "service Derp {\n"
        + "  rpc GetDerp (HerpDerp) returns (HerpDerp);\n"
        + "}\n"
        + "message HerpDerp {}"
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "service Derp {\n"
        + "}\n"
        + "message HerpDerp {}"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.WIRE_INCOMPATIBILITY_VIOLATION))
                .description(equalTo("method removed"))
        )
    );
  }

  @Test
  public void testNoChange() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "service Derp {\n"
        + "  rpc GetDerp (HerpDerp) returns (HerpDerp);\n"
        + "}\n"
        + "message HerpDerp {}"
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "service Derp {\n"
        + "  rpc GetDerp (HerpDerp) returns (HerpDerp);\n"
        + "}\n"
        + "message HerpDerp {}"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, is(empty()));
  }
}