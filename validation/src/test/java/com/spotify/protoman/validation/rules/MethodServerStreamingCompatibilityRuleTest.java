package com.spotify.protoman.validation.rules;

import static com.spotify.protoman.validation.ValidationViolationMatcher.validationViolation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.testutil.DescriptorSetUtils;
import com.spotify.protoman.validation.DefaultSchemaValidator;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import com.spotify.protoman.validation.ViolationType;
import org.junit.Test;

public class MethodServerStreamingCompatibilityRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(MethodServerStreamingCompatibilityRule.create())
      .build();

  @Test
  public void testServerUnaryToServerStreaming() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "service Derp {\n"
        + "  rpc GetDerp (Empty) returns (Empty);\n"
        + "}\n"
        + "message Empty {}\n"
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "service Derp {\n"
        + "  rpc GetDerp (Empty) returns (stream Empty);\n"
        + "}\n"
        + "message Empty {}\n"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo("changed to/from server streaming"))
                .type(equalTo(ViolationType.WIRE_INCOMPATIBILITY_VIOLATION))
        )
    );
  }

  @Test
  public void testServerStreamingToServerUnary() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "service Derp {\n"
        + "  rpc GetDerp (Empty) returns (stream Empty);\n"
        + "}\n"
        + "message Empty {}\n"
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "service Derp {\n"
        + "  rpc GetDerp (Empty) returns (Empty);\n"
        + "}\n"
        + "message Empty {}\n"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo("changed to/from server streaming"))
                .type(equalTo(ViolationType.WIRE_INCOMPATIBILITY_VIOLATION))
        )
    );
  }
}