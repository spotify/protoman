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

public class JavaPackageRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(JavaPackageRule.create())
      .build();

  private static final String TEMPLATE =
      "syntax = 'proto3';\n"
      + "message Derp {\n"
      + "  int32 a_field = %d;\n"
      + "}";

  @Test
  public void newFile_javaOptionPresent() throws Exception {
    final DescriptorSet current = DescriptorSet.empty();
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "option java_package = 'com.foo.bar';"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, is(empty()));
  }

  @Test
  public void newFile_javaOptionMissing() throws Exception {
    final DescriptorSet current = DescriptorSet.empty();
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.BEST_PRACTICE_VIOLATION))
                .description(equalTo("java_package option should be set"))
        )
    );
  }

  @Test
  public void existingFile_javaOptionMissing() throws Exception {
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(candidate, candidate);

    assertThat(violations, is(empty()));
  }

  @Test
  public void existingFile_javaOptionAdded() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "option java_package = 'com.foo.bar';"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION))
                .description(equalTo("Java package changed"))
        )
    );
  }

  @Test
  public void existingFile_javaOptionChanged() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "option java_package = 'com.foo.bar';"
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "option java_package = 'com.a.ńew.java.package';"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION))
                .description(equalTo("Java package changed"))
        )
    );
  }
}
