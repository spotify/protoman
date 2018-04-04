// @flow
import {Action} from 'redux';

export type State = {
  searchTerm: string,
}

export default (state: State = {searchTerm: ''}, action: Action): State => {
  switch (action.type) {
    case 'SEARCH_CHANGE':
      return {...state, searchTerm: action.payload.searchTerm};
    default:
      return state;
  }
};
