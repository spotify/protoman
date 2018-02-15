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
