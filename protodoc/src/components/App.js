// @flow
import {Card, Col, Icon, Input, Layout, Menu, message, Progress, Row} from 'antd';
import React from 'react';
import {connect} from 'react-redux';
import {Route, Switch, withRouter} from "react-router";
import {Link} from "react-router-dom";
import {Action} from 'redux';
import type {State} from '../reducers';
import Entity from './Entity';
import EntityBreadcrumb from './EntityBreadcrumb';
import PackageTree from './PackageTree';

const {Content, Sider} = Layout;
const {Search} = Input;

type Props = {
  navCollapsed: boolean,
  onNavCollapse: boolean => void,
  onSearchChange: string => void,
  onUpdateDescriptors: any => void,
  location: { pathname: string },
}

const Home = connect((state: State) => {
  const entitiesOfType = type => {
    return Array.from(state.descriptors.index.values()).filter(e => e.type === type);
  };
  const countType = type => {
    let entities = entitiesOfType(type);
    return {
      total: entities.length,
      documented: entities.filter(e => {
        if (e.descriptor.$sourceCodeInfo) {
          return e.descriptor.$sourceCodeInfo.leadingComments.trim() !== ''
        } else {
          return false;
        }
      }).length,
    }
  };

  return {
    numPackages: countType('package'),
    numMessages: countType('message'),
    numEnums: countType('enum'),
    numServices: countType('service'),
    numOptions: countType('option'),
  };
})((props) => {
  const percentDocumented = counts => {
    if (counts.total === 0) {
      return 0;
    } else {
      return Math.round(counts.documented / counts.total * 1000) / 10;
    }
  };
  const gridStyle = {
    textAlign: 'center',
    width: '50%',
  };
  return (
    <Content style={{padding: '16px 0'}}>
      <Row gutter={16}>
        <Col span={12}>
          <Card title="Documentation coverage">
            <Card.Grid style={gridStyle}>
              <Progress type="dashboard"
                        percent={percentDocumented(props.numMessages)} />
              <h2>Messages</h2>
            </Card.Grid>
            <Card.Grid style={gridStyle}>
              <Progress type="dashboard"
                        percent={percentDocumented(props.numEnums)} />
              <h2>Enums</h2>
            </Card.Grid>
            <Card.Grid style={gridStyle}>
              <Progress type="dashboard"
                        percent={percentDocumented(props.numServices)} />
              <h2>Services</h2>
            </Card.Grid>
            <Card.Grid style={gridStyle}>
              <Progress type="dashboard"
                        percent={percentDocumented(props.numOptions)} />
              <h2>Options</h2>
            </Card.Grid>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="Statistics">
            <p>
              Number of packages: {props.numPackages.total}
            </p>
            <p>
              Number of messages: {props.numMessages.total}
            </p>
            <p>
              Number of enums: {props.numEnums.total}
            </p>
            <p>
              Number of services: {props.numServices.total}
            </p>
            <p>
              Number of options: {props.numOptions.total}
            </p>
          </Card>
        </Col>
      </Row>
    </Content>
  );
});

const EntityPanel = () => {
  return (
    <div>
      <EntityBreadcrumb />
      <Content>
        <Entity />
      </Content>
    </div>
  );
};

const isLocalhost = Boolean(
  window.location.hostname === 'localhost' ||
  // [::1] is the IPv6 localhost address.
  window.location.hostname === '[::1]' ||
  // 127.0.0.1/8 is considered localhost for IPv4.
  window.location.hostname.match(
    /^127(?:\.(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$/
  )
);

class App extends React.Component<Props> {
  componentWillMount() {
    let url;
    if (isLocalhost) {
      url = 'http://localhost:8080/api/descriptors';
    } else {
      url = '/api/descriptors';
    }
    let delay = 1000;
    const doFetch = () => {
      fetch(url, {headers: {'Accept': 'application/json'}})
        .then(response => response.json())
        .catch(error => {
          console.error('Could not load descriptor data:', error);
          message.error(`Could not fetch schema descriptors from backend: ${error.message}. Will try again in ${Math.round(delay / 100) / 10} seconds.`);
          setTimeout(doFetch, delay);
          delay = Math.min(60 * 1000, delay * 1.5);
        })
        .then(json => this.props.onUpdateDescriptors(json));
    };
    doFetch();
  }

  render() {
    const {navCollapsed, onNavCollapse, onSearchChange, location} = this.props;

    const selectedKeys = [];

    if (location.pathname === '/') {
      selectedKeys.push('home');
    } else {
      selectedKeys.push('packages');
    }

    return (
      <Layout>
        <Sider collapsible
               collapsed={navCollapsed}
               onCollapse={onNavCollapse}
               width={300}
               style={{height: '100vh', overflowY: 'auto'}}>
          <div style={{margin: '8px'}}>
            <Search placeholder='Schema type...'
                    onChange={e => onSearchChange(e.target.value)}
                    onFocus={() => onNavCollapse(false)} />
          </div>
          <Menu theme='dark' mode='inline' selectedKeys={selectedKeys}>
            <Menu.Item key="home">
              <Link to="/">
                <Icon type='home' />
                <span>Overview</span>
              </Link>
            </Menu.Item>
            <Menu.Item key="packages">
              <Link to="/(root)">
                <Icon type='appstore-o' /><span>Packages</span>
              </Link>
            </Menu.Item>
          </Menu>
          {!navCollapsed && <PackageTree />}
        </Sider>
        <Layout
          style={{padding: '0 16px 16px', height: '100vh', overflowY: 'auto'}}>
          <Switch>
            <Route exact path="/" component={Home} />
            <Route path="/:fullName" component={EntityPanel} />
          </Switch>
        </Layout>
      </Layout>
    );
  }
}

const mapStateToProps = (state: State): $Shape<Props> => ({
  navCollapsed: !state.nav.open,
});

const mapDispatchToProps = (dispatch: Action => void): $Shape<Props> => ({
  onNavCollapse: (collapsed) => {
    if (collapsed) {
      dispatch({type: 'NAV_CLOSE'});
    } else {
      dispatch({type: 'NAV_OPEN'});
    }
  },
  onSearchChange: (searchTerm) => {
    dispatch({type: 'SEARCH_CHANGE', payload: {searchTerm}})
  },
  onUpdateDescriptors: (descriptors: any) => {
    dispatch({type: 'DESCRIPTORS_UPDATE', payload: {descriptors}});
  }
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(App));
