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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class FileDescriptor implements GenericDescriptor {

  private final DescriptorProtos.FileDescriptorProto proto;
  @Nullable  private final SourceCodeInfo sourceCodeInfo;

  private final ImmutableList<MessageDescriptor> messageTypes;
  private final ImmutableList<EnumDescriptor> enumTypes;
  private final ImmutableList<ServiceDescriptor> services;
  private final ImmutableList<FileDescriptor> dependencies;
  private final ImmutableList<FileDescriptor> publicDependencies;

  private FileDescriptor(final DescriptorProtos.FileDescriptorProto proto,
                         final PathNode path,
                         final DescriptorPool pool,
                         final Stream<FileDescriptor> dependencies) {
    this.proto = proto;
    this.sourceCodeInfo = path.sourceCodeInfo(this).orElse(null);
    this.dependencies = dependencies.collect(ImmutableList.toImmutableList());
    final List<FileDescriptor> publicDeps = new ArrayList<>();
    proto.getPublicDependencyList().forEach(i -> publicDeps.add(this.dependencies.get(i)));
    this.publicDependencies = ImmutableList.copyOf(publicDeps);

    this.messageTypes = IntStream.range(0, proto.getMessageTypeCount()).mapToObj(idx -> {
      final DescriptorProtos.DescriptorProto messageType = proto.getMessageType(idx);
      return MessageDescriptor.create(messageType, this, idx, path.messageType(idx), pool, null);
    }).collect(ImmutableList.toImmutableList());

    this.enumTypes = IntStream.range(0, proto.getEnumTypeCount()).mapToObj(idx -> {
      final DescriptorProtos.EnumDescriptorProto enumType = proto.getEnumType(idx);
      return EnumDescriptor.create(enumType, this, idx, path.enumType(idx), pool, null);
    }).collect(ImmutableList.toImmutableList());

    // NOTE: services must be created after messages and enums!
    this.services = IntStream.range(0, proto.getServiceCount()).mapToObj(idx -> {
      final DescriptorProtos.ServiceDescriptorProto service = proto.getService(idx);
      return ServiceDescriptor.create(service, this, idx, path.service(idx), pool);
    }).collect(ImmutableList.toImmutableList());

    // Cross-link to resolve types for enum and message fields
    messageTypes.forEach(this::crossLink);
  }

  static FileDescriptor create(final DescriptorProtos.FileDescriptorProto proto,
                               final DescriptorPool pool,
                               final Stream<FileDescriptor> dependencies) {
    final PathNode root;
    if (proto.hasSourceCodeInfo()) {
      root = PathNode.buildPathTree(proto.getSourceCodeInfo());
    } else {
      root = PathNode.emptyTree();
    }

    return new FileDescriptor(proto, root, pool, dependencies);
  }

  public ImmutableList<MessageDescriptor> messageTypes() {
    return messageTypes;
  }

  public ImmutableList<EnumDescriptor> enumTypes() {
    return enumTypes;
  }

  public ImmutableList<ServiceDescriptor> services() {
    return services;
  }

  public ImmutableList<FileDescriptor> dependencies() {
    return dependencies;
  }

  public ImmutableList<FileDescriptor> publicDependencies() {
    return publicDependencies;
  }

  @Override
  public DescriptorProtos.FileDescriptorProto toProto() {
    return proto;
  }

  @Override
  public String name() {
    return proto.getName();
  }

  @Override
  public String fullName() {
    return name();
  }

  public Path filePath() {
    return Paths.get(fullName());
  }

  @Override
  public FileDescriptor file() {
    return this;
  }

  @Override
  public Optional<SourceCodeInfo> sourceCodeInfo() {
    return Optional.ofNullable(sourceCodeInfo);
  }

  public String protoPackage() {
    return proto.getPackage();
  }

  public MessageDescriptor findMessageByName(final String name) {
    return messageTypes().stream()
        .filter(messageDescriptor -> Objects.equals(messageDescriptor.name(), name))
        .findFirst()
        .orElse(null);
  }

  public EnumDescriptor findEnumByName(final String name) {
    return enumTypes().stream()
        .filter(enumDescriptor -> Objects.equals(enumDescriptor.name(), name))
        .findFirst()
        .orElse(null);
  }

  public ServiceDescriptor findServiceByName(final String name) {
    return services().stream()
        .filter(serviceDescriptor -> Objects.equals(serviceDescriptor.name(), name))
        .findFirst()
        .orElse(null);
  }

  //public abstract ImmutableList<EnumDescriptor> enumTypes();

  //public abstract ImmutableList<ServiceDescriptor> services();

  //extensions/options

  public DescriptorProtos.FileOptions options() {
    return toProto().getOptions();
  }

  // If "option java_package" is not specified the java package used for generated code defaults to
  // the protobuf package.
  public String javaPackage() {
    return options().hasJavaPackage() ? options().getJavaPackage() : protoPackage();
  }

  private void crossLink(final MessageDescriptor messageDescriptor) {
    messageDescriptor.fields().forEach(FieldDescriptor::crossLink);
    messageDescriptor.nestedTypes().forEach(this::crossLink);
  }
}
