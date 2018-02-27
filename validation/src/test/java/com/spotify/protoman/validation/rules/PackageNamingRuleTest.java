package com.spotify.protoman.validation.rules;

import static com.spotify.protoman.validation.GenericDescriptorMatcher.genericDescriptor;
import static com.spotify.protoman.validation.ValidationViolationMatcher.validationViolation;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class PackageNamingRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(PackageNamingRule.create())
      .build();

  private static final String TEMPLATE =
      "syntax = 'proto3';\n"
      + "package %s;";

  private static Object[] allowedNames() {
    return new Object[]{
        "foo",
        "foo.bar",
        "protobar",
        "schemabar",
        "barproto",
        "barschema"
    };
  }

  private static Object[] disallowedNames() {
    return new Object[]{
        new Object[]{"FOOBAR", "package name should be all-lower case"},
        new Object[]{"FOO.BAR", "package name should be all-lower case"},
        new Object[]{"Foo.Bar", "package name should be all-lower case"},
        new Object[]{"foo.bar.proto", "package name should not contain 'schema' or 'proto'"},
        new Object[]{"foo.bar.proto.x", "package name should not contain 'schema' or 'proto'"},
        new Object[]{"foo.bar.schema", "package name should not contain 'schema' or 'proto'"},
        new Object[]{"foo.bar.schema.x", "package name should not contain 'schema' or 'proto'"},
    };
  }

  @Parameters(method = "allowedNames")
  @Test
  public void testAllowedName(final String name) throws Exception {
    final DescriptorSet current = DescriptorSet.empty();
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, name)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, is(empty()));
  }

  @Parameters(method = "disallowedNames")
  @Test
  public void testDisallowedName(final String name, final String expectedDescription)
      throws Exception {
    final DescriptorSet current = DescriptorSet.empty();
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, name)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo(expectedDescription))
                .type(equalTo(ViolationType.STYLE_GUIDE_VIOLATION))
                .current(nullValue(GenericDescriptor.class))
                .candidate(genericDescriptor())
        )
    );
  }
}