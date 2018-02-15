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
