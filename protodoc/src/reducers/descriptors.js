// @flow
import {matchPath} from "react-router";
import {Action} from 'redux';
import type {Entity, EntityType} from "../model";

export type State = {
  data: any | null,
  root: Entity | null,
  index: Map<string, Entity>,
  pendingEntityFullName: string | null,
  entity: Entity | null,
}

const createEntity = (fullName: string, type: EntityType, descriptor): Entity => ({
  fullName,
  type,
  descriptor,
  children: [],
  sourceFiles: [],
});

const buildEntities = (type: EntityType, fileName: string, prefix: string, parent: Entity | null, descriptor, index: Map<string, Entity>) => {
  const entity: Entity = createEntity(prefix + descriptor.name, type, descriptor);

  if (type === 'message') {
    const subPrefix = prefix + entity.descriptor.name + '.';
    descriptor.nestedType.forEach(messageType => buildEntities('message', fileName, subPrefix, entity, messageType, index));
    descriptor.enumType.forEach(enumType => buildEntities('enum', fileName, subPrefix, entity, enumType, index));
  }

  if (parent) {
    parent.children.push(entity);
    // TODO: do insertion sort
    parent.children.sort((a, b) => a.fullName.localeCompare(b.fullName));
  }

  index.set(entity.fullName, entity);
  entity.sourceFiles.push(fileName);
  return entity;
};

const ensurePackage = (root: Entity, fileName: string, pkg: string, index: Map<string, Entity>): Entity => {
  let pkgEntity = root;

  pkg.split('.').forEach(part => {
    const fullName = pkgEntity.fullName + '.' + part;
    // TODO: index packages by name
    const existingPackage = pkgEntity.children.find(e => e.fullName === fullName);
    if (existingPackage) {
      pkgEntity = existingPackage;
    } else {
      const newPackage = createEntity(fullName, 'package', {name: part});
      index.set(fullName, newPackage);
      pkgEntity.children.push(newPackage);
      // TODO: do insertion sort
      pkgEntity.children.sort((a, b) => a.fullName.localeCompare(b.fullName));
      pkgEntity = newPackage;
    }
  });

  pkgEntity.sourceFiles.push(fileName);
  return pkgEntity;
};

const buildEntitiesFromFiles = (data, index: Map<string, Entity>): Entity => {
  const root = createEntity('', 'package', {name: '(root)'});

  data.file.forEach(file => {
    const pkg = ensurePackage(root, file.name, file.package, index);

    let prefix: string;
    if (file.package === '') {
      prefix = '.';
    } else {
      prefix = '.' + file.package + '.';
    }
    file.messageType.forEach(messageType => buildEntities('message', file.name, prefix, pkg, messageType, index));
    file.enumType.forEach(enumType => buildEntities('enum', file.name, prefix, pkg, enumType, index));
    file.service.forEach(service => buildEntities('service', file.name, prefix, pkg, service, index));
    file.extension.forEach(service => buildEntities('option', file.name, prefix, pkg, service, index));
  });

  index.set(root.fullName, root);
  return root;
};

const populateSourceCodeInfo = (data) => {
  data.file.forEach(file => {
    file.sourceCodeInfo.location.forEach(location => {
      const path = location.path;

      // This state machine is glorious.  The 'path' is an array of numbers; the first number is a
      // field number in the descriptor proto schema (e.g. FileDescriptorProto.message_type = 4) and
      // if that field is repeated, the next number is an index into the repeated array.

      // We don't follow all the paths here; only the ones that are interesting.  Consider extending
      // the state machine if you want source information for more elements.
      let subject = file;
      let subjectType = 'file';
      for (let i = 0; i < path.length; i++) {
        switch (subjectType) {
          case 'file': {
            const fieldId = path[i];
            switch (fieldId) {
              case 4: // message_type
                i += 1;
                subject = subject.messageType[path[i]];
                subjectType = 'message';
                break;
              case 5: // enum_type
                i += 1;
                subject = subject.enumType[path[i]];
                subjectType = 'enum';
                break;
              case 6: // service
                i += 1;
                subject = subject.service[path[i]];
                subjectType = 'service';
                break;
              case 7: // extension
                i += 1;
                subject = subject.extension[path[i]];
                subjectType = 'option';
                break;
              case 1: // name
              case 2: // package
              case 3: // dependency
              case 8: // options
              case 10: // public_dependency
              case 11: // weak_dependency
              case 12: // syntax
                return; // don't care
              case 9: // source_code_info
                console.warn('Too much meta; we do not need source code info about the source code info');
                return;
              default:
                console.warn('Unrecognized file sourceCodeInfo field tag', fieldId);
                return;
            }
            break;
          }
          case 'message': {
            const fieldId = path[i];
            switch (fieldId) {
              case 2: // field
                i += 1;
                subject = subject.field[path[i]];
                subjectType = 'field';
                break;
              case 3: // nested_type
                i += 1;
                subject = subject.nestedType[path[i]];
                subjectType = 'message';
                break;
              case 4: // enum_type
                i += 1;
                subject = subject.enumType[path[i]];
                subjectType = 'enum';
                break;
              case 1: // name
              case 5: // extension_range
              case 6: // extension
              case 7: // options
              case 8: // oneof_decl
              case 9: // reserved_range
              case 10: // reserved_name
                return; // don't care
              default:
                console.warn('Unrecognized message sourceCodeInfo field tag', fieldId);
                return;
            }
            break;
          }
          case 'enum': {
            const fieldId = path[i];
            switch (fieldId) {
              case 2: // value
                i += 1;
                subject = subject.value[path[i]];
                subjectType = 'enum_value';
                break;
              case 1: // name
              case 3: // options
              case 4: // reserved_range
              case 5: // reserved_name
                return; // don't care
              default:
                console.warn('Unrecognized enum sourceCodeInfo field tag', fieldId);
                return;
            }
            break;
          }
          case 'field': {
            return; // note: return because this is a terminating path branch
          }
          case 'enum_value': {
            return; // note: return because this is a terminating path branch
          }
          case 'service': {
            const fieldId = path[i];
            switch (fieldId) {
              case 2: // method
                i += 1;
                subject = subject.method[path[i]];
                subjectType = 'method';
                break;
              case 1: // name
              case 3: // options
                return; // don't care
              default:
                console.warn('Unrecognized service sourceCodeInfo field tag', fieldId);
                return;
            }
            break;
          }
          case 'method': {
            return; // note: return because this is a terminating path branch
          }
          case 'option': {
            return; // note: return because this is a terminating path branch
          }
          default:
            console.warn('Unimplemented subject type', subjectType);
        }
      }

      if (subject) {
        subject.$sourceCodeInfo = {
          start: {
            line: location.span[0],
            column: location.span[1],
          },
          end: {
            line: location.span.length === 3 ? location.span[0] : location.span[2],
            column: location.span.length === 3 ? location.span[2] : location.span[3],
          },
          fileName: file.name,
          leadingComments: location.leadingComments,
          trailingComments: location.trailingComments,
          detachedLeadingComments: location.detachedLeadingComments,
        }
      }
    });
  });
};

export default (state: State = {
  data: null,
  root: null,
  index: new Map(),
  pendingEntityFullName: null,
  entity: null
}, action: Action): State => {
  switch (action.type) {
    case 'DESCRIPTORS_UPDATE':
      const data = action.payload.descriptors;
      populateSourceCodeInfo(data);
      const index = new Map();
      const root = buildEntitiesFromFiles(data, index);

      if (state.pendingEntityFullName) {
        return {
          ...state,
          data,
          root,
          index,
          entity: index.get(state.pendingEntityFullName),
          pendingEntityFullName: null
        };
      } else {
        return {...state, data, root, index};
      }
    case 'ENTITY_CHANGE':
      return {...state, entity: action.payload.entity};
    case '@@router/LOCATION_CHANGE':
      const match = matchPath(action.payload.pathname, {path: '/:fullName'});
      if (match) {
        let fullName = match.params.fullName;

        if (fullName === '(root)') {
          fullName = '';
        } else {
          fullName = '.' + fullName;
        }

        if (state.index.size > 0) {
          let entity = state.index.get(fullName);
          return {...state, entity: entity, pendingEntityFullName: null};
        } else {
          return {...state, pendingEntityFullName: fullName};
        }
      } else {
        return state;
      }
    default:
      return state;
  }
};
