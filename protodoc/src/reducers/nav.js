// @flow
import {Action} from 'redux';

export type State = {
  open: boolean,
}

export default (state: State = {open: true}, action: Action): State => {
  switch (action.type) {
    case 'NAV_OPEN':
      return {...state, open: true};
    case 'NAV_CLOSE':
      return {...state, open: false};
    default:
      return state;
  }
};
