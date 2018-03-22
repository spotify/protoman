/*-
 * -\-\-
 * protoman-descriptor-model
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

package com.spotify.protoman.descriptor;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.DescriptorProtos;

public enum FieldType {
  MESSAGE(DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE),
  GROUP(DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP),
  ENUM(DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM),
  BYTES(DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES),
  STRING(DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING),
  INT32(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32),
  SINT32(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32),
  UINT32(DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32),
  FIXED32(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32),
  SFIXED32(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32),
  INT64(DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64),
  SINT64(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64),
  UINT64(DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64),
  FIXED64(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64),
  SFIXED64(DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64),
  BOOL(DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL),
  DOUBLE(DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE),
  FLOAT(DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT);

  private DescriptorProtos.FieldDescriptorProto.Type protoType;

  FieldType(final DescriptorProtos.FieldDescriptorProto.Type protoType) {
    this.protoType = protoType;
  }

  public static FieldType fromProtoType(
      final DescriptorProtos.FieldDescriptorProto.Type protoType) {
    for (final FieldType fieldType : FieldType.values()) {
      if (fieldType.protoType == protoType) {
        return fieldType;
      }
    }

    throw new RuntimeException("Unknown field type " + protoType);
  }

  public DescriptorProtos.FieldDescriptorProto.Type protoType() {
    return protoType;
  }

  public static boolean isWireCompatible(final FieldType a, final FieldType b) {
    return WIRE_COMPATIBLE_TYPES.contains(FieldTypePair.create(a, b));
  }

  @AutoValue
  static abstract class FieldTypePair {
    abstract FieldType a();

    abstract FieldType b();

    static FieldTypePair create(final FieldType a, final FieldType b) {
      return new AutoValue_FieldType_FieldTypePair(a, b);
    }
  }

  // Set of type pairs that are wire-compatible. E.g. (int32, uint32) being in the set means
  // a field can be changed from int32 to uint32 and vice versa (it will also contain (uint32,
  // int32)).
  private static final ImmutableSet<FieldTypePair> WIRE_COMPATIBLE_TYPES;

  static {
    // See https://developers.google.com/protocol-buffers/docs/proto#updating
    // for information on type wire compatibility.
    final ImmutableSet.Builder<FieldTypePair> builder = ImmutableSet.builder();
    // "int32, uint32, int64, uint64, and bool are all compatible"
    addWireCompatibleTypes(builder, INT32, UINT32, INT64, UINT64, BOOL);
    // "sint32 and sint64 are compatible with each other but are not compatible with the other
    // integer types."
    addWireCompatibleTypes(builder, SINT32, SINT64);
    // "fixed32 is compatible with sfixed32," ...
    addWireCompatibleTypes(builder, FIXED32, SFIXED32);
    // ... "and fixed64 with sfixed64"
    addWireCompatibleTypes(builder, FIXED64, SFIXED64);
    // "enum is compatible with int32, uint32, int64, and uint64 in terms of wire format (note that
    // values will be truncated if they don't fit), but be aware that client code may treat them
    // differently when the message is deserialized."
    addWireCompatibleTypes(builder, INT32, UINT32, INT64, UINT64, ENUM);
    // "string and bytes are compatible as long as the bytes are valid UTF-8."
    addWireCompatibleTypes(builder, STRING, BYTES);
    // "Embedded messages are compatible with bytes if the bytes contain an encoded version of
    // the message."
    addWireCompatibleTypes(builder, MESSAGE, BYTES);
    WIRE_COMPATIBLE_TYPES = builder.build();
  }

  private static void addWireCompatibleTypes(final ImmutableSet.Builder<FieldTypePair> builder,
                                             final FieldType... types) {
    for (final FieldType t1 : types) {
      for (final FieldType t2 : types) {
        builder.add(FieldTypePair.create(t1, t2));
        if (!t1.equals(t2)) {
          builder.add(FieldTypePair.create(t2, t1));
        }
      }
    }
  }
}
