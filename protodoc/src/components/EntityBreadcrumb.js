// @flow
import {Breadcrumb} from 'antd';
import React from 'react';
import {connect} from 'react-redux';
import {Link} from "react-router-dom";
import {Action} from 'redux';
import type {Entity} from "../model";
import type {State} from '../reducers';

type Props = {
  value: Entity | null;
}

type Segment = { name: string, fullName: string }

const EntityBreadcrumb = (props: Props) => {
  const value: Entity | null = props.value;
  if (value) {
    const parents: Segment[] = [];
    let fullNamePrefix = '.';

    value.fullName.split('.').filter(part => part !== '').forEach(name => {
      const fullName = fullNamePrefix + name;
      parents.push({name, fullName});
      fullNamePrefix = fullName + '.';
    });

    parents.splice(-1, 1);

    return (
      <Breadcrumb style={{margin: '16px 0'}}>
        {parents.map(p => (
          <Breadcrumb.Item key={p.fullName}><Link
            to={'/' + p.fullName.substr(1)}><code>{p.name}</code></Link></Breadcrumb.Item>))}
        <Breadcrumb.Item><code><strong>{value.descriptor.name}</strong></code>
          &nbsp;({value.type})</Breadcrumb.Item>
      </Breadcrumb>
    );
  } else {
    return null;
  }
};

const mapStateToProps = (state: State): $Shape<Props> => ({
  value: state.descriptors.entity,
});

const mapDispatchToProps = (dispatch: Action => void): $Shape<Props> => ({});

export default connect(mapStateToProps, mapDispatchToProps)(EntityBreadcrumb);
