export const AUTH_AUTHORIZATION_STATUS_CHANGED = 'AUTH_AUTHORIZATION_STATUS_CHANGED';

export const AUTH_LOGIN_REDIRECT_SUCCEEDED = 'AUTH_CODE_SUCCEEDED';
export const AUTH_LOGIN_REDIRECT_FAILED = 'AUTH_CODE_FAILED';
export const AUTH_LOGIN_SUCCEEDED = 'AUTH_LOGIN_SUCCEEDED';
export const AUTH_LOGIN_FAILED = 'AUTH_LOGIN_FAILED';

export const AUTH_INFO_SUCCEEDED = 'AUTH_INFO_SUCCEEDED';
export const AUTH_INFO_FAILED = 'AUTH_INFO_FAILED';

export const AUTH_LOGOUT_SUCCEEDED = 'AUTH_LOGOUT_SUCCEEDED';
export const AUTH_LOGOUT_FAILED = 'AUTH_LOGOUT_FAILED';

export const AUTH_USER_INACTIVE = 'AUTH_USER_INACTIVE';
export const AUTH_USER_DEV = 'AUTH_USER_DEVELOPER';
export const GET_SYSTEM_INFO = 'GET_SYSTEM_INFO';


export function authRedirectSucceeded(code) {
  return {
    type: AUTH_LOGIN_REDIRECT_SUCCEEDED,
    payload: {
      code: code
    }
  };
}

export function authRedirectFailed(message) {
  return {
    type: AUTH_LOGIN_REDIRECT_FAILED,
    payload: {
      error: {
        severity: "Error",
        message: message
      }
    }
  };
}

export function authLoginSucceeded(authToken, expiresIn) {
  return {
    type: AUTH_LOGIN_SUCCEEDED,
    payload: {
      authToken: authToken,
      expiresIn: expiresIn
    }
  };
}

export function getSystemInfo(sys) {
  return {
    type: GET_SYSTEM_INFO,
    payload: {
      sys: sys
    }
  };
}

export function authLoginFailed(message) {
  return {
    type: AUTH_LOGIN_FAILED,
    payload: {
      error: {
        severity: "Error",
        message: message
      }
    }
  };
}

export function authInfoSucceeded(name, preferredUsername, email, roles,primary_role) {
  return dispatch => {
    dispatch({
      type: AUTH_INFO_SUCCEEDED,
      payload: {
        user: {
          name: name,
          preferredUsername: preferredUsername,
          email: email,
          roles: roles,
          primary_role:primary_role
        },
      }
    });
  }
}

export function authInfoFailed(message) {
  return dispatch => {
    dispatch({
      type: AUTH_INFO_FAILED,
      payload: {
        error: {
          severity: "Error",
          message: message
        }
      }
    });
  }
}

export function authLogoutSucceeded() {
  return {
    type: AUTH_LOGOUT_SUCCEEDED
  };
}

export function authLogoutFailed(message) {
  return {
    type: AUTH_LOGOUT_FAILED,
    payload: {
      error: {
        severity: "Error",
        message: message
      }
    }
  };
}

export function userInactiveState(inActiveTime) {
  return {
    type: AUTH_USER_INACTIVE,
    payload: {
      inActiveTime: inActiveTime
    }
  };
}

export function userIsDev() {
  return {
    type: AUTH_USER_DEV,
    payload: {
      isDev: true
    }
  };
}

function authAuthorizationStatusChanged(status) {
  return {
    type: AUTH_AUTHORIZATION_STATUS_CHANGED,
    payload: {
      authorizationStatus: status
    }
  }
}

export function authAuthorizationSuccessful() {
  return authAuthorizationStatusChanged('successful');
}

export function authAuthorizationPending() {
  return authAuthorizationStatusChanged('pending');
}

export function authAuthorizationError() {
  return authAuthorizationStatusChanged('error');
}

export function authAuthorizationReset() {
  return {
    type: AUTH_AUTHORIZATION_STATUS_CHANGED,
    payload: {
      code: null,
      authToken: null,
      expiresIn: null,
      authorizationStatus: 'forbidden',
      isAuthenticated: false,
      isAuthorized: false,
      isLoggedIn: false,
      user: {
        name: null,
        preferredUsername: null,
        email: null,
        roles: null
      }
    }
  }
}