// @flow
import createHistory from 'history/createBrowserHistory'
import React from 'react';
import ReactDOM from 'react-dom';
import {Provider} from 'react-redux';
import {ConnectedRouter, routerMiddleware} from 'react-router-redux';
import {applyMiddleware, createStore} from 'redux';
import App from './components/App';
import './index.css';
import reducers from './reducers';
import registerServiceWorker from './registerServiceWorker';

const history = createHistory();
const store = createStore(reducers, applyMiddleware(routerMiddleware(history)));
const element = document.getElementById('root');

if (element) {
  ReactDOM.render(
    <Provider store={store}>
      <ConnectedRouter history={history}>
        <App />
      </ConnectedRouter>
    </Provider>,
    element);
} else {
  console.error('Could not find root element');
}

registerServiceWorker();
