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

public class OneofDescriptor extends DescriptorBase<DescriptorProtos.OneofDescriptorProto> {

  private final MessageDescriptor containingType;

  private OneofDescriptor(final DescriptorProtos.OneofDescriptorProto proto,
                          final FileDescriptor file,
                          final int index,
                          final PathNode path,
                          final MessageDescriptor containingType) {
    super(proto, file, index, path, containingType.fullName() + "." + proto.getName());
    this.containingType = containingType;
  }

  static OneofDescriptor create(final DescriptorProtos.OneofDescriptorProto proto,
                                final FileDescriptor file,
                                final int index,
                                final PathNode path,
                                final MessageDescriptor containingType) {
    return new OneofDescriptor(proto, file, index, path, containingType);
  }

  @Override
  public String name() {
    return toProto().getName();
  }

  public MessageDescriptor getType() {
    return containingType;
  }

  public DescriptorProtos.OneofOptions options() {
    return toProto().getOptions();
  }
}
