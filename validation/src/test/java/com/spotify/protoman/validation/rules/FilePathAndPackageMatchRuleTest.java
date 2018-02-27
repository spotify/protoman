package com.spotify.protoman.validation.rules;

import static com.spotify.protoman.validation.GenericDescriptorMatcher.genericDescriptor;
import static com.spotify.protoman.validation.ValidationViolationMatcher.validationViolation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
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
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class FilePathAndPackageMatchRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(FilePathAndPackageMatchRule.create())
      .build();

  @Test
  public void testPackageAndPathMatches_newFile() throws Exception {
    final DescriptorSet current = DescriptorSet.empty();
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testPackageAndPathMatches_existingFile() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
            "foo/bar/a.proto",
            "syntax = 'proto3';\n"
            + "package bad.package;"
        );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testPackageAndPathMismatch_newFile() throws Exception {
    final DescriptorSet current = DescriptorSet.empty();
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.baz;"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo("proto file path must match package name"))
                .type(equalTo(ViolationType.BEST_PRACTICE_VIOLATION))
                .current(nullValue(GenericDescriptor.class))
                .candidate(genericDescriptor())
        )
    );
  }

  @Test
  public void testPackageAndPathMismatch_existingFile() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;"
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package bad.package;"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo("proto file path must match package name"))
                .type(equalTo(ViolationType.BEST_PRACTICE_VIOLATION))
                .current(genericDescriptor())
                .candidate(genericDescriptor())
        )
    );
  }
}