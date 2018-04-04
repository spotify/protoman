// @flow
import {Spin, Tree} from 'antd';
import React from 'react';
import {connect} from 'react-redux';
import {push} from 'react-router-redux';
import {Action} from 'redux';
import scrollIntoView from 'scroll-into-view'
import type {Entity as ModelEntity} from "../model";
import type {State} from '../reducers';
import './PackageTree.css';

const {TreeNode} = Tree;

type Props = {
  root: ModelEntity | null,
  entityIndex: Map<string, ModelEntity>,
  searchTerm: string,
  onEntitySelected: ModelEntity => void,
  selectedEntity: Entity | null,
}

type LocalState = {
  expandedKeys: string[],
  autoExpandParent: false,
}

type TreeState = {
  searchFragments: string[],
  focusedKey: string | null,
  setFocusedNode: Node => void,
};

const createChildren = (entity: ModelEntity, treeState: TreeState) => {
  const children = entity.children;
  return children.length > 0 ? children.map(n => createTreeNode(n, treeState)) : undefined;
};

// TODO(dflemstr): this rendering step replicates the actual search algorithm in findMatchingKeys,
// can it be consolidated?
const createTreeNode = (entity: ModelEntity, treeState: TreeState) => {

  const name: string = entity.descriptor.name;
  const key: string = entity.fullName;

  let title;
  let nextSearchState: TreeState;
  let searchFragments = treeState.searchFragments;
  if (searchFragments.length > 0) {
    const searchFragment = searchFragments[0];
    const index = name.indexOf(searchFragment);
    if (index >= 0) {
      const before = name.substr(0, index);
      const after = name.substr(index + searchFragment.length);
      title = <code style={{color: 'rgba(0, 0, 0, .25)'}}>{before}<span
        style={{color: '#000'}}>{searchFragment}</span>{after}</code>;
      nextSearchState = {...treeState, searchFragments: searchFragments.slice(1)};
    } else {
      title = <code style={{color: 'rgba(0, 0, 0, .25)'}}>{name}</code>;
      nextSearchState = treeState;
    }
  } else {
    title = <code>{name}</code>;
    nextSearchState = treeState;
  }

  return (
    <TreeNode title={<span ref={(n) => {
      if (key === treeState.focusedKey) {
        treeState.setFocusedNode(n);
      }
    }}>{title}</span>}
              key={key}
              className={'components-package-tree-node-type-' + entity.type}
              children={createChildren(entity, nextSearchState)} />
  );
};

const fragment = (searchTerm) => {
  return searchTerm.split('.').filter(s => s.length !== 0);
};

const findMatchingKeys = (entity: ModelEntity, searchState: TreeState): string[] => {
  const {searchFragments} = searchState;
  if (searchFragments.length === 0) {
    return [];
  } else {
    const searchFragment = searchFragments[0];
    const name: string = entity.descriptor.name;
    const index = name.indexOf(searchFragment);
    let shouldIncludeSelf = false;

    let nextSearchState: TreeState;
    if (index >= 0) {
      nextSearchState = {...searchState, searchFragments: searchFragments.slice(1)};
      shouldIncludeSelf = true; // Because it matches a fragment
    } else {
      nextSearchState = searchState;
    }

    const result = [];
    entity.children.forEach(child => {
      const childKeys = findMatchingKeys(child, nextSearchState);
      if (childKeys.length > 0) {
        shouldIncludeSelf = true; // Because it's at least the parent of a match
        result.push(...childKeys);
      }
    });

    if (shouldIncludeSelf) {
      result.push(entity.fullName);
    }

    return result;
  }
};

class PackageTree extends React.Component<Props, LocalState> {
  state: LocalState = {
    expandedKeys: undefined,
    autoExpandParent: true,
  };

  focusedKey: string | null = null;
  lastFocusedKey: string | null = null;
  selectedNode: Node | null = null;

  componentWillReceiveProps(nextProps: Props) {
    if (nextProps.searchTerm !== this.props.searchTerm && nextProps.root) {
      this.onSearchTermChange(nextProps.searchTerm, nextProps.root);
    }
  }

  componentDidUpdate() {
    const node = this.selectedNode;
    if (node && (!this.props.searchFragments || this.props.searchFragments.length === 0)
      && this.focusedKey !== this.lastFocusedKey) {
      scrollIntoView(node);
      this.lastFocusedKey = this.focusedKey;
    }
  }

  onSearchTermChange = (searchTerm: string, root: ModelEntity) => {
    const searchFragments = fragment(searchTerm);
    let expandedKeys = findMatchingKeys(root, {searchFragments});
    this.setState({...this.state, expandedKeys, autoExpandParent: true});
  };

  onTreeExpand = (expandedKeys: string[]) => {
    this.setState({...this.state, expandedKeys, autoExpandParent: false});
  };

  render() {
    const {root, entityIndex, searchTerm, onEntitySelected} = this.props;

    if (root) {
      const searchFragments = fragment(searchTerm);
      const focusedKey = this.props.selectedEntity ? this.props.selectedEntity.fullName : null;
      this.focusedKey = focusedKey;
      const focusedKeys = focusedKey ? [focusedKey] : [];
      this.selectedNode = null;
      const setFocusedNode = (n) => {
        this.selectedNode = n;
      };
      const children = createChildren(root, {searchFragments, focusedKey, setFocusedNode});

      let expandedKeys = this.state.expandedKeys;
      let autoExpandParent = this.state.autoExpandParent;

      if (focusedKey) {
        const parentKey = focusedKey.substr(0, focusedKey.lastIndexOf('.'));
        if (!expandedKeys || expandedKeys.indexOf(parentKey) < 0) {
          if (!expandedKeys) {
            expandedKeys = [];
          }
          expandedKeys.push(parentKey);
          autoExpandParent = true;
        }
      }

      return (
        <div style={{background: '#fff', marginBottom: '48px'}}>
          <Tree
            showLine
            className='components-package-tree'
            onExpand={this.onTreeExpand}
            onSelect={(keys) => {
              if (keys.length > 0) {
                onEntitySelected(entityIndex.get(keys[0]));
              }
            }}
            defaultExpandedKeys={focusedKeys}
            defaultSelectedKeys={focusedKeys}
            selectedKeys={focusedKeys}
            expandedKeys={expandedKeys}
            autoExpandParent={autoExpandParent}
            children={children} />
        </div>
      );
    } else {
      return <div style={{
        background: '#fff',
        marginBottom: '48px',
        textAlign: 'center',
        padding: '30px 50px'
      }}><Spin size="large" /></div>;
    }
  }
}

const mapStateToProps = (state: State): $Shape<Props> => ({
  searchTerm: state.search.searchTerm,
  root: state.descriptors.root,
  entityIndex: state.descriptors.index,
  selectedEntity: state.descriptors.entity,
});

const mapDispatchToProps = (dispatch: Action => void): $Shape<Props> => ({
  onEntitySelected: (entity) => {
    dispatch({type: 'ENTITY_CHANGE', payload: {entity}});
    if (entity.fullName === '') {
      dispatch(push('/(root)'));
    } else {
      dispatch(push('/' + entity.fullName.substr(1)));
    }
  }
});

export default connect(mapStateToProps, mapDispatchToProps)(PackageTree);
