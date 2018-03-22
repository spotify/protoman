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

public class MethodDescriptor extends DescriptorBase<DescriptorProtos.MethodDescriptorProto> {

  private final ServiceDescriptor service;
  private final MessageDescriptor inputType;
  private final MessageDescriptor outputType;

  private MethodDescriptor(final DescriptorProtos.MethodDescriptorProto proto,
                           final FileDescriptor file,
                           final int index,
                           final PathNode path,
                           final DescriptorPool pool,
                           final ServiceDescriptor service) {
    super(proto, file, index, path, service.fullName() + "." + proto.getName());
    this.service = service;
    // Message are available when methods descriptors are created because
    // 1) All dependencies of a file is created before we create descriptors from the current file
    // 2) For a given file, message descriptors are created /before/ service and method descriptors
    this.inputType = pool.findMessageType(proto.getInputType());
    this.outputType = pool.findMessageType(proto.getOutputType());
  }

  static MethodDescriptor create(final DescriptorProtos.MethodDescriptorProto proto,
                                 final FileDescriptor file,
                                 final int index,
                                 final PathNode path,
                                 final DescriptorPool pool,
                                 final ServiceDescriptor service) {
    return new MethodDescriptor(proto, file, index, path, pool, service);
  }

  @Override
  public String name() {
    return toProto().getName();
  }

  public ServiceDescriptor service() {
    return service;
  }

  public MessageDescriptor inputType() {
    return inputType;
  }

  public MessageDescriptor outputType() {
    return outputType;
  }

  public DescriptorProtos.MethodOptions options() {
    return toProto().getOptions();
  }

  public DescriptorProtos.MethodOptions.IdempotencyLevel idempotencyLevel() {
    return options().getIdempotencyLevel();
  }
}
