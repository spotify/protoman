// @flow

export type EntityType = 'package' | 'message' | 'enum' | 'service' | 'option';

export type Entity = {
  type: EntityType,
  // if type === 'message', this is a DescriptorProto,
  // if type === 'enum', this is a EnumDescriptorProto,
  // if type === 'service', this is a ServiceDescriptorProto,
  // if type === 'option', this is a FieldDescriptorProto
  descriptor: {
    name: string
  },
  fullName: string,
  children: Entity[],
  sourceFiles: string[],
}
