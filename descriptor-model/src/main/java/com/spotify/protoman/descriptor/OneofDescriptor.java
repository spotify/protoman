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
