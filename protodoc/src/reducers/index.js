// @flow
import {routerReducer as routing} from 'react-router-redux';
import {combineReducers} from 'redux';
import type {State as DescriptorsState} from './descriptors';
import descriptors from './descriptors';
import type {State as NavState} from './nav';
import nav from './nav';
import type {State as SearchState} from './search';
import search from './search';

export type State = {
  descriptors: DescriptorsState,
  nav: NavState,
  routing: any, // Don't care
  search: SearchState,
}

export default combineReducers({
  descriptors,
  nav,
  routing,
  search,
});
