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
public class MethodIdempotencyChangeRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(MethodIdempotencyChangeRule.create())
      .build();

  private static Object[] allowedIdempotencyChanges() {
    return new Object[]{
        new Object[]{"IDEMPOTENCY_UNKNOWN", "IDEMPOTENT"},
        new Object[]{"IDEMPOTENCY_UNKNOWN", "NO_SIDE_EFFECTS"},
        new Object[]{"IDEMPOTENT", "NO_SIDE_EFFECTS"}
    };
  }

  private static Object[] disallowedIdempotencyChanges() {
    return new Object[]{
        new Object[]{"IDEMPOTENT", "IDEMPOTENCY_UNKNOWN"},
        new Object[]{"NO_SIDE_EFFECTS", "IDEMPOTENCY_UNKNOWN"},
        new Object[]{"NO_SIDE_EFFECTS", "IDEMPOTENT"}
    };
  }

  @Parameters(method = "allowedIdempotencyChanges")
  @Test
  public void testIdempotencyChange_allowed(final String from, final String to) throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        String.format(
            "syntax = 'proto3';\n"
            + "service Foo {\n"
            + "  rpc GetFoo(Derp) returns (Derp) { option idempotency_level = %s; };\n"
            + "}\n"
            + "message Derp {}"
            , from)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        String.format(
            "syntax = 'proto3';\n"
            + "service Foo {\n"
            + "  rpc GetFoo(Derp) returns (Derp) { option idempotency_level = %s; };\n"
            + "}\n"
            + "message Derp {}"
            , to)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, is(empty()));
  }

  @Parameters(method = "disallowedIdempotencyChanges")
  @Test
  public void testIdempotencyChange_disallowed(final String from, final String to)
      throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        String.format(
            "syntax = 'proto3';\n"
            + "service Foo {\n"
            + "  rpc GetFoo(Derp) returns (Derp) { option idempotency_level = %s; };\n"
            + "}\n"
            + "message Derp {}"
            , from)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        String.format(
            "syntax = 'proto3';\n"
            + "service Foo {\n"
            + "  rpc GetFoo(Derp) returns (Derp) { option idempotency_level = %s; };\n"
            + "}\n"
            + "message Derp {}"
            , to)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.BEST_PRACTICE_VIOLATION))
                .description(equalTo("Idempotency level changed to less strict"))
        )
    );
  }
}
