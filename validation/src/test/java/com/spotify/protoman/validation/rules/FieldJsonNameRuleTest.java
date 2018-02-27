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

import com.google.common.collect.ImmutableList;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.testutil.DescriptorSetUtils;
import com.spotify.protoman.validation.DefaultSchemaValidator;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import com.spotify.protoman.validation.ViolationType;
import org.junit.Test;

public class FieldJsonNameRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(FieldJsonNameRule.create())
      .build();

  @Test
  public void testJsonNameUnchanged() throws Exception {
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "message AMessage {\n"
        + "  int32 a_field = 1;"
        + "}"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(candidate, candidate);

    assertThat(violations, is(empty()));
  }

  @Test
  public void testJsonNameChanged() throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "message AMessage {\n"
        + "  int32 a_field = 1;\n"
        + "}"
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "foo/bar/a.proto",
        "syntax = 'proto3';\n"
        + "package foo.bar;\n"
        + "message AMessage {\n"
        + "  // oopsie\n"
        + "  int32 a_field = 1 [json_name = 'afield'];\n"
        + "}"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .type(equalTo(ViolationType.WIRE_INCOMPATIBILITY_VIOLATION))
                .description(equalTo("json name changed (aField -> afield)"))
                .current(genericDescriptor()
                    .sourceCodeInfo(optionalWithValue(sourceCodeInfo().start(filePosition()
                        .line(4)
                        .column(3)))))
                .candidate(genericDescriptor()
                    .sourceCodeInfo(optionalWithValue(sourceCodeInfo().start(filePosition()
                        .line(5)
                        .column(3)))))
        )
    );
  }
}