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

import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.stream.IntStream;

public class MessageDescriptor extends DescriptorBase<DescriptorProtos.DescriptorProto> {

  @Nullable private final MessageDescriptor containingType;
  private final ImmutableList<MessageDescriptor> nestedTypes;
  private final ImmutableList<EnumDescriptor> enumTypes;
  private final ImmutableList<FieldDescriptor> fields;
  private final ImmutableList<OneofDescriptor> oneofs;

  private MessageDescriptor(final DescriptorProtos.DescriptorProto proto,
                            final FileDescriptor file,
                            final int index,
                            final PathNode path,
                            final DescriptorPool pool,
                            @Nullable final MessageDescriptor containingType) {
    super(proto, file, index, path, Util.computeFullName(file, containingType, proto.getName()));

    this.containingType = containingType;

    pool.put(this);

    this.nestedTypes = IntStream.range(0, proto.getNestedTypeCount()).mapToObj(idx -> {
      final DescriptorProtos.DescriptorProto d = proto.getNestedType(idx);
      return MessageDescriptor.create(d, file, idx, path.messageType(idx), pool, this);
    }).collect(ImmutableList.toImmutableList());

    this.enumTypes = IntStream.range(0, proto.getEnumTypeCount()).mapToObj(idx -> {
      final DescriptorProtos.EnumDescriptorProto d = proto.getEnumType(idx);
      return EnumDescriptor.create(d, file, idx, path.enumType(idx), pool, this);
    }).collect(ImmutableList.toImmutableList());

    this.fields = IntStream.range(0, proto.getFieldCount()).mapToObj(idx -> {
      final DescriptorProtos.FieldDescriptorProto d = proto.getField(idx);
      return FieldDescriptor.create(d, file, idx, path.field(idx), pool, this);
    }).collect(ImmutableList.toImmutableList());

    this.oneofs = IntStream.range(0, proto.getOneofDeclCount()).mapToObj(idx -> {
      final DescriptorProtos.OneofDescriptorProto d = proto.getOneofDecl(idx);
      return OneofDescriptor.create(d, file, idx, path.oneof(idx), this);
    }).collect(ImmutableList.toImmutableList());
  }

  static MessageDescriptor create(final DescriptorProtos.DescriptorProto proto,
                                  final FileDescriptor file,
                                  final int index,
                                  final PathNode path,
                                  final DescriptorPool pool,
                                  @Nullable final MessageDescriptor containingType) {
    return new MessageDescriptor(proto, file, index, path, pool, containingType);
  }

  public ImmutableList<MessageDescriptor> nestedTypes() {
    return nestedTypes;
  }

  public ImmutableList<EnumDescriptor> enumTypes() {
    return enumTypes;
  }

  public ImmutableList<FieldDescriptor> fields() {
    return fields;
  }

  public ImmutableList<OneofDescriptor> oneofs() {
    return oneofs;
  }

  @Override
  public String name() {
    return toProto().getName();
  }

  public MessageDescriptor containingType() {
    return containingType;
  }

  public MessageDescriptor findMessageByName(final String name) {
    return nestedTypes().stream()
        .filter(d -> Objects.equals(d.name(), name))
        .findFirst()
        .orElse(null);
  }

  public EnumDescriptor findEnumByName(final String name) {
    return enumTypes().stream()
        .filter(d -> Objects.equals(d.name(), name))
        .findFirst()
        .orElse(null);
  }

  public FieldDescriptor findFieldByName(final String name) {
    return fields().stream()
        .filter(d -> Objects.equals(d.name(), name))
        .findFirst()
        .orElse(null);
  }

  public FieldDescriptor findFieldByNumber(final int number) {
    return fields().stream()
        .filter(d -> d.number() == number)
        .findFirst()
        .orElse(null);
  }

  public DescriptorProtos.MessageOptions options() {
    return toProto().getOptions();
  }

  public boolean isReservedNumber(final int number) {
    for (final DescriptorProtos.DescriptorProto.ReservedRange reservedRange :
        toProto().getReservedRangeList()) {
      if (number >= reservedRange.getStart() && number < reservedRange.getEnd()) {
        return true;
      }
    }
    return false;
  }

  public boolean isReservedName(final String name) {
    return toProto().getReservedNameList().contains(name);
  }

  // TODO: extensions/options
}
