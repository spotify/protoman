package com.spotify.protoman.descriptor;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class EnumDescriptor extends DescriptorBase<DescriptorProtos.EnumDescriptorProto> {

  @Nullable private final MessageDescriptor containingType;
  private final ImmutableList<EnumValueDescriptor> values;

  private EnumDescriptor(final DescriptorProtos.EnumDescriptorProto proto,
                         final FileDescriptor file,
                         final int index,
                         final PathNode path,
                         final DescriptorPool pool,
                         @Nullable final MessageDescriptor containingType) {
    super(proto, file, index, path, Util.computeFullName(file, containingType, proto.getName()));

    this.containingType = containingType;

    pool.put(this);

    this.values = IntStream.range(0, proto.getValueCount()).mapToObj(idx -> {
      final DescriptorProtos.EnumValueDescriptorProto d = proto.getValue(idx);
      return EnumValueDescriptor.create(d, file, idx, path.enumValue(idx), this);
    }).collect(ImmutableList.toImmutableList());
  }

  static EnumDescriptor create(final DescriptorProtos.EnumDescriptorProto proto,
                               final FileDescriptor file,
                               final int index,
                               final PathNode path,
                               final DescriptorPool pool,
                               @Nullable final MessageDescriptor containingType) {
    return new EnumDescriptor(proto, file, index, path, pool, containingType);
  }

  public ImmutableList<EnumValueDescriptor> values() {
    return values;
  }

  @Nullable public MessageDescriptor containingType() {
    return containingType;
  }

  @Override
  public String name() {
    return toProto().getName();
  }

  public EnumValueDescriptor findValueByName(final String name) {
    return values().stream()
        .filter(d -> Objects.equals(d.name(), name))
        .findFirst()
        .orElse(null);
  }

  public Stream<EnumValueDescriptor> findValuesByNumber(final int number) {
    return values().stream().filter(d -> d.number() == number);
  }

  public DescriptorProtos.EnumOptions options() {
    return toProto().getOptions();
  }
}
