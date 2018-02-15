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
