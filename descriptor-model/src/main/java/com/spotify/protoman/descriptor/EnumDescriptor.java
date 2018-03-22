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
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class EnumDescriptor extends DescriptorBase<DescriptorProtos.EnumDescriptorProto> {

  @Nullable private final MessageDescriptor containingType;
  private final ImmutableList<EnumValueDescriptor> values;

  private EnumDescriptor(final DescriptorProtos.EnumDescriptorProto proto,
                         final FileDescriptor file,
                         final int index,
                         final PathNode path,
                         final DescriptorPool pool,
                         @Nullable final MessageDescriptor containingType) {
    super(proto, file, index, path, Util.computeFullName(file, containingType, proto.getName()));

    this.containingType = containingType;

    pool.put(this);

    this.values = IntStream.range(0, proto.getValueCount()).mapToObj(idx -> {
      final DescriptorProtos.EnumValueDescriptorProto d = proto.getValue(idx);
      return EnumValueDescriptor.create(d, file, idx, path.enumValue(idx), this);
    }).collect(ImmutableList.toImmutableList());
  }

  static EnumDescriptor create(final DescriptorProtos.EnumDescriptorProto proto,
                               final FileDescriptor file,
                               final int index,
                               final PathNode path,
                               final DescriptorPool pool,
                               @Nullable final MessageDescriptor containingType) {
    return new EnumDescriptor(proto, file, index, path, pool, containingType);
  }

  public ImmutableList<EnumValueDescriptor> values() {
    return values;
  }

  @Nullable public MessageDescriptor containingType() {
    return containingType;
  }

  @Override
  public String name() {
    return toProto().getName();
  }

  public EnumValueDescriptor findValueByName(final String name) {
    return values().stream()
        .filter(d -> Objects.equals(d.name(), name))
        .findFirst()
        .orElse(null);
  }

  public Stream<EnumValueDescriptor> findValuesByNumber(final int number) {
    return values().stream().filter(d -> d.number() == number);
  }

  public DescriptorProtos.EnumOptions options() {
    return toProto().getOptions();
  }

  public boolean isReservedNumber(final int number) {
    for (final DescriptorProtos.EnumDescriptorProto.EnumReservedRange reservedRange :
        toProto().getReservedRangeList()) {
      // NOTE(staffan): EnumReservedRange's end is inclusive. End for ReservedRange (for messages)
      // is exclusive. :(
      if (number >= reservedRange.getStart() && number <= reservedRange.getEnd()) {
        return true;
      }
    }
    return false;
  }

  public boolean isReservedName(final String name) {
    return toProto().getReservedNameList().contains(name);
  }
}
