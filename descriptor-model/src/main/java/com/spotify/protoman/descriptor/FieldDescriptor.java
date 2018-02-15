package com.spotify.protoman.descriptor;

import com.google.protobuf.DescriptorProtos;

public class FieldDescriptor extends DescriptorBase<DescriptorProtos.FieldDescriptorProto> {

  private DescriptorPool pool;
  private final MessageDescriptor containingType;
  private final FieldType type;
  // Set during cross-linking
  private MessageDescriptor messageType = null;
  private EnumDescriptor enumType = null;
  private final String jsonName;

  private FieldDescriptor(final DescriptorProtos.FieldDescriptorProto proto,
                          final FileDescriptor file,
                          final int index,
                          final PathNode path,
                          final DescriptorPool pool,
                          final MessageDescriptor containingType) {
    super(proto, file, index, path, containingType.fullName() + "." + proto.getName());
    this.pool = pool;
    this.containingType = containingType;
    this.type = FieldType.fromProtoType(proto.getType());
    this.jsonName =
        proto.hasJsonName() ? proto.getJsonName() : fieldNameToJsonName(proto.getName());
  }

  public static FieldDescriptor create(final DescriptorProtos.FieldDescriptorProto proto,
                                       final FileDescriptor file,
                                       final int index,
                                       final PathNode path,
                                       final DescriptorPool pool,
                                       final MessageDescriptor containingType) {
    return new FieldDescriptor(proto, file, index, path, pool, containingType);
  }

  @Override
  public String name() {
    return toProto().getName();
  }

  public MessageDescriptor containingType() {
    return messageType;
  }

  public FieldType type() {
    return this.type;
  }

  public String typeName() {
    return toProto().getTypeName();
  }

  public MessageDescriptor messageType() {
    return messageType;
  }

  public EnumDescriptor enumType() {
    return enumType;
  }

  public int number() {
    return toProto().getNumber();
  }

  public String jsonName() {
    return jsonName;
  }

  public boolean isRequired() {
    return toProto().getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED;
  }

  public boolean isOptional() {
    return toProto().getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL;
  }

  public boolean isRepeated() {
    return toProto().getLabel() == DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED;
  }

  private static String fieldNameToJsonName(final String name) {
    final StringBuilder result = new StringBuilder(name.length());
    boolean isNextUpperCase = false;
    for (int i = 0; i < name.length(); i++) {
      Character ch = name.charAt(i);
      if (ch == '_') {
        isNextUpperCase = true;
      } else if (isNextUpperCase) {
        result.append(Character.toUpperCase(ch));
        isNextUpperCase = false;
      } else {
        result.append(ch);
      }
    }
    return result.toString();
  }

  public DescriptorProtos.FieldOptions options() {
    return toProto().getOptions();
  }

  void crossLink() {
    if (type() == FieldType.MESSAGE) {
      this.messageType = pool.findMessageType(typeName());
    } else if (type() == FieldType.ENUM) {
      this.enumType = pool.findEnumType(typeName());
    }
  }
}
