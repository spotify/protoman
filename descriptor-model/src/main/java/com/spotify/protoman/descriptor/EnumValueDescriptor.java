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

import com.google.protobuf.DescriptorProtos;

public class EnumValueDescriptor extends DescriptorBase<DescriptorProtos.EnumValueDescriptorProto> {

  private final EnumDescriptor containingEnum;

  private EnumValueDescriptor(final DescriptorProtos.EnumValueDescriptorProto proto,
                              final FileDescriptor file,
                              final int index,
                              final PathNode path,
                              final EnumDescriptor containingEnum) {
    super(proto, file, index, path, containingEnum.fullName() + "." + proto.getName());
    this.containingEnum = containingEnum;
  }

  static EnumValueDescriptor create(final DescriptorProtos.EnumValueDescriptorProto proto,
                                    final FileDescriptor file,
                                    final int index,
                                    final PathNode path,
                                    final EnumDescriptor containingEnum) {
    return new EnumValueDescriptor(proto, file, index, path, containingEnum);
  }

  @Override
  public String name() {
    return toProto().getName();
  }

  public EnumDescriptor getType() {
    return containingEnum;
  }

  public int number() {
    return toProto().getNumber();
  }

  public DescriptorProtos.EnumValueOptions options() {
    return toProto().getOptions();
  }
}
