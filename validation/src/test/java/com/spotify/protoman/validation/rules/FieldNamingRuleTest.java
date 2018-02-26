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
import com.spotify.protoman.descriptor.DescriptorBuilder;
import com.spotify.protoman.descriptor.DescriptorSet;
import com.spotify.protoman.descriptor.GenericDescriptor;
import com.spotify.protoman.descriptor.ProtocDescriptorBuilder;
import com.spotify.protoman.validation.DefaultSchemaValidator;
import com.spotify.protoman.validation.SchemaValidator;
import com.spotify.protoman.validation.ValidationViolation;
import com.spotify.protoman.validation.ViolationType;
import java.nio.file.Paths;
import java.util.stream.Stream;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class FieldNamingRuleTest {

  private static final DescriptorBuilder.Factory factory = ProtocDescriptorBuilder.factoryBuilder()
      .build();

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(FieldNamingRule.create())
      .build();

  @Test
  public void testFieldWithLowerSnakeCase() throws Exception {
    final DescriptorSet current = buildDs(
        "a.proto",
        "syntax = 'proto3';\n"
        + "message Derp {\n"
        + "  string lower_snake_case = 1;"
        + "}"
    );
    final DescriptorSet candidate = buildDs(
        "a.proto",
        "syntax = 'proto3';\n"
        + "message Derp {\n"
        + "  string lower_snake_case = 1;"
        + "  int32 anotherfield = 2;"
        + "}"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, is(empty()));
  }

  @Test
  @Parameters(method = "testParams")
  public void testNewFieldViolatingStyleGuide(final String fieldName) throws Exception {
    final DescriptorSet current = DescriptorSet.empty();
    final DescriptorSet candidate = buildDs(
        "a.proto",
        "syntax = 'proto3';\n"
        + "message Derp {\n"
        + String.format("  int32 %s = 1;\n", fieldName)
        + "}"
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo("field name should be lower_snake_case"))
                .type(equalTo(ViolationType.STYLE_GUIDE_VIOLATION))
                .current(nullValue(GenericDescriptor.class))
                .candidate(genericDescriptor()
                    .sourceCodeInfo(optionalWithValue(
                        sourceCodeInfo().start(
                            filePosition()
                                .line(3)
                                .column(3)
                        )))
                )
        )
    );
  }

  @Test
  @Parameters(method = "testParams")
  public void testExistingFieldViolatingStyleGuide(final String fieldName) throws Exception {
    final DescriptorSet candidate = buildDs(
        "a.proto",
        "syntax = 'proto3';\n"
        + "message Derp {\n"
        + String.format("  int32 %s = 1;\n", fieldName)
        + "}"
    );

    final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
        .addRule(FieldNamingRule.create())
        .build();
    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(candidate, candidate);

    assertThat(
        violations,
        is(empty())
    );
  }

  private static Object[] testParams() {
    return new Object[]{
        "lowerCamelCase",
        "UpperCamelCase",
        "UPPER_SNAKE_CASE",
        "MIXED_snake_Case",
        "UPPERCASE",
        "Snake_Camel_Case"
    };
  }

  private static DescriptorSet buildDs(final String path, final String content) throws Exception {
    final DescriptorBuilder descriptorBuilder = factory.newDescriptorBuilder();
    descriptorBuilder.setProtoFile(Paths.get(path), content);
    return descriptorBuilder.buildDescriptor(Stream.of(Paths.get(path))).descriptorSet();
  }
}