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

import com.google.protobuf.Message;
import java.util.Optional;

abstract class DescriptorBase<ProtoType extends Message> implements GenericDescriptor {

  private final ProtoType proto;
  private FileDescriptor file;
  private int index;
  private final SourceCodeInfo sourceCodeInfo;
  private final String fullName;

  protected DescriptorBase(final ProtoType proto,
                           final FileDescriptor file,
                           final int index,
                           final PathNode path,
                           final String fullName) {
    this.proto = proto;
    this.file = file;
    this.index = index;
    this.sourceCodeInfo = path.sourceCodeInfo(file).orElse(null);
    this.fullName = fullName;
  }

  @Override
  public ProtoType toProto() {
    return proto;
  }

  @Override
  public Optional<SourceCodeInfo> sourceCodeInfo() {
    return Optional.ofNullable(sourceCodeInfo);
  }

  @Override
  public String fullName() {
    return fullName;
  }

  @Override
  public FileDescriptor file() {
    return file;
  }

  public int index() {
    return index;
  }
}
