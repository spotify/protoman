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
import java.util.stream.IntStream;

public class ServiceDescriptor extends DescriptorBase<DescriptorProtos.ServiceDescriptorProto> {

  private final ImmutableList<MethodDescriptor> methods;

  private ServiceDescriptor(final DescriptorProtos.ServiceDescriptorProto proto,
                            final FileDescriptor file,
                            final int index,
                            final PathNode path,
                            final DescriptorPool pool) {
    super(proto, file, index, path, Util.computeFullName(file, null, proto.getName()));

    this.methods = IntStream.range(0, proto.getMethodCount()).mapToObj(
        idx -> {
          final DescriptorProtos.MethodDescriptorProto d = proto.getMethod(idx);
          return MethodDescriptor.create(d, file, idx, path.method(idx), pool, this);
        }
    ).collect(ImmutableList.toImmutableList());
  }

  static ServiceDescriptor create(final DescriptorProtos.ServiceDescriptorProto proto,
                                  final FileDescriptor file,
                                  final int index,
                                  final PathNode path,
                                  final DescriptorPool pool) {
    return new ServiceDescriptor(proto, file, index, path, pool);
  }

  public ImmutableList<MethodDescriptor> methods() {
    return methods;
  }

  @Override
  public String name() {
    return toProto().getName();
  }

  public DescriptorProtos.ServiceOptions options() {
    return toProto().getOptions();
  }
}
