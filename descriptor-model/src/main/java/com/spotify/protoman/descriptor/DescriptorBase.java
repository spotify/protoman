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
