/*-
 * -\-\-
 * protoman-validation
 * --
 * Copyright (C) 2016 - 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

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
public class FieldTypeCompatibilityRuleTest {

  private final SchemaValidator schemaValidator = DefaultSchemaValidator.builder()
      .addRule(FieldTypeCompatibilityRule.create())
      .build();

  private static final String TEMPLATE =
      "syntax = 'proto3';\n"
      + "message Derp {\n"
      // %s will be substituted for currentType and candidateType
      + "  %s herp = 1;\n"
      + "}\n"
      + "message Empty {}\n"
      + "message A {\n"
      + "  int32 a = 1;\n"
      + "}\n"
      + "message SuperTypeOfA {\n"
      + "  int32 a = 1;\n"
      + "  string b = 2;\n"
      + "}\n"
      + "message SameAsA {\n"
      + "  int32 a = 1;\n"
      + "}\n"
      + "message WireCompatWithA {\n"
      + "  bool a = 1;\n"
      + "}\n"
      + "message WireIncompatWithA {\n"
      + "  int32 abc = 2;\n"
      + "}\n"
      + "message WireCompatFieldNameChanged {\n"
      + "  int32 a_different_name = 1;\n"
      + "}\n"
      + "enum AnEnum {\n"
      + "  UNKNOWN = 0;\n"
      + "  VAL1 = 1;\n"
      + "}\n"
      + "enum ACompatEnum {\n"
      + "  UNSPECIFIED_A = 0;\n"
      + "  VALUE1 = 1;\n"
      + "}\n"
      + "enum AnotherCompatEnum {\n"
      + "  UNSPECIFIED_B = 0;\n"
      + "  VAL_1 = 1;\n"
      + "  VAL_2 = 2;\n"
      + "}\n"
      + "enum IncompatEnum {\n"
      + "  UNSPECIFIED_C = 0;\n"
      + "}\n"
      + "message NestedType {\n"
      + "  A a = 1;\n"
      + "  int32 b = 2;\n"
      + "  AnEnum c = 3;\n"
      + "}\n"
      + "message CompatNestedType {\n"
      + "  SuperTypeOfA a = 1;\n"
      + "  bool b = 2;\n"
      + "  ACompatEnum c = 3;\n"
      + "}\n"
      + "message IncompatNestedType {\n"
      + "  WireIncompatWithA a = 1;\n"
      + "  int32 b = 2;\n"
      + "  ACompatEnum c = 3;\n"
      + "}\n"
      ;

  @Parameters(method = "complexTypeChanges")
  @Test
  public void testComplexFieldTypeChange(
      final String currentType, final String candidateType,
      final ViolationType expectedViolationType,
      final String expectedDescription) throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, currentType)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, candidateType)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo(expectedDescription))
                .type(equalTo(expectedViolationType))
        )
    );
  }

  @Parameters(method = "wireCompatiblePrimitiveTypeChanges")
  @Test
  public void testWireCompatibleFieldTypeChange(
      final String currentType, final String candidateType) throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, currentType)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, candidateType)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo("field type changed (wire-compat)"))
                .type(equalTo(ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION))
        )
    );
  }

  @Parameters(method = "wireIncompatiblePrimitiveTypeChanges")
  @Test
  public void testWireIncompatibleFieldTypeChange(
      final String currentType, final String candidateType) throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, currentType)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, candidateType)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(
        violations,
        contains(
            validationViolation()
                .description(equalTo(
                    String.format("wire-incompatible field type change %s -> %s",
                        currentType.toUpperCase(), candidateType.toUpperCase())
                ))
                .type(equalTo(ViolationType.WIRE_INCOMPATIBILITY_VIOLATION))
        )
    );
  }

  @Parameters(method = "typeUnchanged")
  @Test
  public void testFieldTypeUnchanged(final String typeName) throws Exception {
    final DescriptorSet current = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, typeName)
    );
    final DescriptorSet candidate = DescriptorSetUtils.buildDescriptorSet(
        "a.proto", String.format(TEMPLATE, typeName)
    );

    final ImmutableList<ValidationViolation> violations =
        schemaValidator.validate(current, candidate);

    assertThat(violations, is(empty()));
  }

  private static Object[] typeUnchanged() {
    return new Object[]{
        "AnEnum",
        "A",
        "bytes",
        "string",
        "int32",
        "sint32",
        "uint32",
        "fixed32",
        "sfixed32",
        "int64",
        "sint64",
        "uint64",
        "fixed64",
        "sfixed64",
        "bool",
        "double",
        "float"
    };
  }

  private static Object[] complexTypeChanges() {
    return new Object[][]{
        // bytes <-> message
        new Object[]{
            "A", "bytes",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        new Object[]{
            "bytes", "A",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        // enum <-> int32
        new Object[]{
            "AnEnum", "int32",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        new Object[]{
            "int32", "AnEnum",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        // Message type substitution changes
        new Object[]{
            "A", "SameAsA",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        new Object[]{
            "A", "WireCompatWithA",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        new Object[]{
            "A", "SuperTypeOfA",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        new Object[]{
            "A", "Empty",
            ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
            "message types A and Empty are not interchangable, field a does exist in the new "
            + "message type used"
        },
        new Object[]{
            "A", "WireIncompatWithA",
            ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
            "message types A and WireIncompatWithA are not interchangable, field a does exist in "
            + "the new message type used"
        },
        new Object[]{
            "A", "WireCompatFieldNameChanged",
            ViolationType.FIELD_MASK_INCOMPATIBILITY,
            "message types A and WireCompatFieldNameChanged are not interchangable, field a has "
            + "different name in new message type used (current=a, candidate=a_different_name)"
        },
        // Enum type substitution changes
        new Object[]{
            "AnEnum", "ACompatEnum",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        new Object[]{
            "AnEnum", "AnotherCompatEnum",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        new Object[]{
            "AnEnum", "IncompatEnum",
            ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
            "enum types AnEnum and IncompatEnum are not interchangable, value with number 1 does "
            + "exist in the new type used"
        },
        // Complex, nested field types
        new Object[]{
            "NestedType", "CompatNestedType",
            ViolationType.GENERATED_SOURCE_CODE_INCOMPATIBILITY_VIOLATION,
            "field type changed (wire-compat)"
        },
        new Object[]{
            "NestedType", "IncompatNestedType",
            ViolationType.WIRE_INCOMPATIBILITY_VIOLATION,
            // TODO(staffan): Should give a better description here, referencing NestedType and
            // IncompatNestedType -- not the types of the incompatible fields
            "message types A and WireIncompatWithA are not interchangable, field a does exist in "
            + "the new message type used"
        },
    };
  }

  // All wire-compatible primitive type changes
  private static Object[] wireCompatiblePrimitiveTypeChanges() {
    return new Object[][]{
        new Object[]{ "bool", "int32" },
        new Object[]{ "bool", "uint32" },
        new Object[]{ "bool", "int64" },
        new Object[]{ "bool", "uint64" },
        new Object[]{ "int32", "bool" },
        new Object[]{ "int32", "uint32" },
        new Object[]{ "int32", "int64" },
        new Object[]{ "int32", "uint64" },
        new Object[]{ "uint32", "bool" },
        new Object[]{ "uint32", "int32" },
        new Object[]{ "uint32", "int64" },
        new Object[]{ "uint32", "uint64" },
        new Object[]{ "int64", "bool" },
        new Object[]{ "int64", "int32" },
        new Object[]{ "int64", "uint32" },
        new Object[]{ "int64", "uint64" },
        new Object[]{ "uint64", "bool" },
        new Object[]{ "uint64", "int32" },
        new Object[]{ "uint64", "uint32" },
        new Object[]{ "uint64", "int64" },
        new Object[]{ "sint32", "sint64" },
        new Object[]{ "sint64", "sint32" },
        new Object[]{ "string", "bytes" },
        new Object[]{ "bytes", "string" },
        new Object[]{ "fixed32", "sfixed32" },
        new Object[]{ "sfixed32", "fixed32" },
        new Object[]{ "fixed64", "sfixed64" },
        new Object[]{ "sfixed64", "fixed64" }
    };
  }

  // All wire-incompatible primitive type changes
  private static Object[] wireIncompatiblePrimitiveTypeChanges() {
    return new Object[][]{
        new Object[]{ "bytes", "int32" },
        new Object[]{ "bytes", "sint32" },
        new Object[]{ "bytes", "uint32" },
        new Object[]{ "bytes", "fixed32" },
        new Object[]{ "bytes", "sfixed32" },
        new Object[]{ "bytes", "int64" },
        new Object[]{ "bytes", "sint64" },
        new Object[]{ "bytes", "uint64" },
        new Object[]{ "bytes", "fixed64" },
        new Object[]{ "bytes", "sfixed64" },
        new Object[]{ "bytes", "bool" },
        new Object[]{ "bytes", "double" },
        new Object[]{ "bytes", "float" },
        new Object[]{ "string", "int32" },
        new Object[]{ "string", "sint32" },
        new Object[]{ "string", "uint32" },
        new Object[]{ "string", "fixed32" },
        new Object[]{ "string", "sfixed32" },
        new Object[]{ "string", "int64" },
        new Object[]{ "string", "sint64" },
        new Object[]{ "string", "uint64" },
        new Object[]{ "string", "fixed64" },
        new Object[]{ "string", "sfixed64" },
        new Object[]{ "string", "bool" },
        new Object[]{ "string", "double" },
        new Object[]{ "string", "float" },
        new Object[]{ "int32", "bytes" },
        new Object[]{ "int32", "string" },
        new Object[]{ "int32", "sint32" },
        new Object[]{ "int32", "fixed32" },
        new Object[]{ "int32", "sfixed32" },
        new Object[]{ "int32", "sint64" },
        new Object[]{ "int32", "fixed64" },
        new Object[]{ "int32", "sfixed64" },
        new Object[]{ "int32", "double" },
        new Object[]{ "int32", "float" },
        new Object[]{ "sint32", "bytes" },
        new Object[]{ "sint32", "string" },
        new Object[]{ "sint32", "int32" },
        new Object[]{ "sint32", "uint32" },
        new Object[]{ "sint32", "fixed32" },
        new Object[]{ "sint32", "sfixed32" },
        new Object[]{ "sint32", "int64" },
        new Object[]{ "sint32", "uint64" },
        new Object[]{ "sint32", "fixed64" },
        new Object[]{ "sint32", "sfixed64" },
        new Object[]{ "sint32", "bool" },
        new Object[]{ "sint32", "double" },
        new Object[]{ "sint32", "float" },
        new Object[]{ "uint32", "bytes" },
        new Object[]{ "uint32", "string" },
        new Object[]{ "uint32", "sint32" },
        new Object[]{ "uint32", "fixed32" },
        new Object[]{ "uint32", "sfixed32" },
        new Object[]{ "uint32", "sint64" },
        new Object[]{ "uint32", "fixed64" },
        new Object[]{ "uint32", "sfixed64" },
        new Object[]{ "uint32", "double" },
        new Object[]{ "uint32", "float" },
        new Object[]{ "fixed32", "bytes" },
        new Object[]{ "fixed32", "string" },
        new Object[]{ "fixed32", "int32" },
        new Object[]{ "fixed32", "sint32" },
        new Object[]{ "fixed32", "uint32" },
        new Object[]{ "fixed32", "int64" },
        new Object[]{ "fixed32", "sint64" },
        new Object[]{ "fixed32", "uint64" },
        new Object[]{ "fixed32", "fixed64" },
        new Object[]{ "fixed32", "sfixed64" },
        new Object[]{ "fixed32", "bool" },
        new Object[]{ "fixed32", "double" },
        new Object[]{ "fixed32", "float" },
        new Object[]{ "sfixed32", "bytes" },
        new Object[]{ "sfixed32", "string" },
        new Object[]{ "sfixed32", "int32" },
        new Object[]{ "sfixed32", "sint32" },
        new Object[]{ "sfixed32", "uint32" },
        new Object[]{ "sfixed32", "int64" },
        new Object[]{ "sfixed32", "sint64" },
        new Object[]{ "sfixed32", "uint64" },
        new Object[]{ "sfixed32", "fixed64" },
        new Object[]{ "sfixed32", "sfixed64" },
        new Object[]{ "sfixed32", "bool" },
        new Object[]{ "sfixed32", "double" },
        new Object[]{ "sfixed32", "float" },
        new Object[]{ "int64", "bytes" },
        new Object[]{ "int64", "string" },
        new Object[]{ "int64", "sint32" },
        new Object[]{ "int64", "fixed32" },
        new Object[]{ "int64", "sfixed32" },
        new Object[]{ "int64", "sint64" },
        new Object[]{ "int64", "fixed64" },
        new Object[]{ "int64", "sfixed64" },
        new Object[]{ "int64", "double" },
        new Object[]{ "int64", "float" },
        new Object[]{ "sint64", "bytes" },
        new Object[]{ "sint64", "string" },
        new Object[]{ "sint64", "int32" },
        new Object[]{ "sint64", "uint32" },
        new Object[]{ "sint64", "fixed32" },
        new Object[]{ "sint64", "sfixed32" },
        new Object[]{ "sint64", "int64" },
        new Object[]{ "sint64", "uint64" },
        new Object[]{ "sint64", "fixed64" },
        new Object[]{ "sint64", "sfixed64" },
        new Object[]{ "sint64", "bool" },
        new Object[]{ "sint64", "double" },
        new Object[]{ "sint64", "float" },
        new Object[]{ "uint64", "bytes" },
        new Object[]{ "uint64", "string" },
        new Object[]{ "uint64", "sint32" },
        new Object[]{ "uint64", "fixed32" },
        new Object[]{ "uint64", "sfixed32" },
        new Object[]{ "uint64", "sint64" },
        new Object[]{ "uint64", "fixed64" },
        new Object[]{ "uint64", "sfixed64" },
        new Object[]{ "uint64", "double" },
        new Object[]{ "uint64", "float" },
        new Object[]{ "fixed64", "bytes" },
        new Object[]{ "fixed64", "string" },
        new Object[]{ "fixed64", "int32" },
        new Object[]{ "fixed64", "sint32" },
        new Object[]{ "fixed64", "uint32" },
        new Object[]{ "fixed64", "fixed32" },
        new Object[]{ "fixed64", "sfixed32" },
        new Object[]{ "fixed64", "int64" },
        new Object[]{ "fixed64", "sint64" },
        new Object[]{ "fixed64", "uint64" },
        new Object[]{ "fixed64", "bool" },
        new Object[]{ "fixed64", "double" },
        new Object[]{ "fixed64", "float" },
        new Object[]{ "sfixed64", "bytes" },
        new Object[]{ "sfixed64", "string" },
        new Object[]{ "sfixed64", "int32" },
        new Object[]{ "sfixed64", "sint32" },
        new Object[]{ "sfixed64", "uint32" },
        new Object[]{ "sfixed64", "fixed32" },
        new Object[]{ "sfixed64", "sfixed32" },
        new Object[]{ "sfixed64", "int64" },
        new Object[]{ "sfixed64", "sint64" },
        new Object[]{ "sfixed64", "uint64" },
        new Object[]{ "sfixed64", "bool" },
        new Object[]{ "sfixed64", "double" },
        new Object[]{ "sfixed64", "float" },
        new Object[]{ "bool", "bytes" },
        new Object[]{ "bool", "string" },
        new Object[]{ "bool", "sint32" },
        new Object[]{ "bool", "fixed32" },
        new Object[]{ "bool", "sfixed32" },
        new Object[]{ "bool", "sint64" },
        new Object[]{ "bool", "fixed64" },
        new Object[]{ "bool", "sfixed64" },
        new Object[]{ "bool", "double" },
        new Object[]{ "bool", "float" },
        new Object[]{ "double", "bytes" },
        new Object[]{ "double", "string" },
        new Object[]{ "double", "int32" },
        new Object[]{ "double", "sint32" },
        new Object[]{ "double", "uint32" },
        new Object[]{ "double", "fixed32" },
        new Object[]{ "double", "sfixed32" },
        new Object[]{ "double", "int64" },
        new Object[]{ "double", "sint64" },
        new Object[]{ "double", "uint64" },
        new Object[]{ "double", "fixed64" },
        new Object[]{ "double", "sfixed64" },
        new Object[]{ "double", "bool" },
        new Object[]{ "double", "float" },
        new Object[]{ "float", "bytes" },
        new Object[]{ "float", "string" },
        new Object[]{ "float", "int32" },
        new Object[]{ "float", "sint32" },
        new Object[]{ "float", "uint32" },
        new Object[]{ "float", "fixed32" },
        new Object[]{ "float", "sfixed32" },
        new Object[]{ "float", "int64" },
        new Object[]{ "float", "sint64" },
        new Object[]{ "float", "uint64" },
        new Object[]{ "float", "fixed64" },
        new Object[]{ "float", "sfixed64" },
        new Object[]{ "float", "bool" },
        new Object[]{ "float", "double" }
    };
  }
}