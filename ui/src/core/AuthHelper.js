import {
  authAuthorizationError,
  authAuthorizationPending,
  authAuthorizationReset,
  authAuthorizationSuccessful,
  authInfoFailed,
  authInfoSucceeded,
  authLoginFailed,
  authLoginSucceeded,
  authLogoutFailed,
  authLogoutSucceeded,
  authRedirectFailed,
  authRedirectSucceeded,
} from '../actions/AuthActions';

const authTokenKey = "AUTH_TOKEN";
const idTokenKey = "ID_TOKEN";
const authExpirationDateKey = "AUTH_EXPIRATION_DATE";

const ROOT_REDIRECT_URL = '#/';

export const USER_AUTHORIZED_ROLES = [
  'dlx.conductor.admin',
  'dlx.conductor.user'
];

export const USER_AUTHORIZED_ROLES_SET = new Set(USER_AUTHORIZED_ROLES);

const getURLParams = (param) => {
  var results = new RegExp('[?&]' + param + '=([^&#]*)').exec(window.location.href);
  if (results === null) {
    return null;
  } else {
    return decodeURI(results[1]) || 0;
  }
};

const saveRedirectURI = () => {
  var redirectURI = window.location.hash;
  redirectURI = redirectURI.substr(0, redirectURI.lastIndexOf('?'));

  // No need to set for root login url
  if (ROOT_REDIRECT_URL !== redirectURI) {
    sessionStorage.setItem('redirectURI', redirectURI);
  }
};

export const authLogin = (isAuthenticated) => {
  return (dispatch) => {
    // check if the validation of this user failed
    const error = getURLParams('error');
    if (typeof error !== 'undefined' && error === 'access_denied') {
      console.error("failed to redirect user to dashboard, invalid okta access");
      removeTokensLocally();
      dispatch(authRedirectFailed(error));
      window.location.href = '/Unauthorized.html';
      return;
    }

    const code = getURLParams('code');
    if (code === null && !isAuthenticated) {
      const authTokenVal = getLocalAuthToken();
      const idTokenVal = getLocalIdAuthToken();

      if (authTokenVal && idTokenVal && idTokenVal !== undefined) {
        authUserInfo(idTokenVal, authTokenVal)(dispatch);
        dispatch(authLoginSucceeded(authTokenVal, 0));
        var redirectURI = sessionStorage.getItem('redirectURI');
        if (redirectURI != null) {
          window.location.href = '/' + redirectURI;
          sessionStorage.clear();
        }
      } else {
        saveRedirectURI();

        let params = {
          redirectURI: window.location.origin
        };

        fetch('/api/auth/login', {
          method: 'POST',
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(params)
        }).then(response => {
          if (response.ok) {
            return response.json();
          }
        }).then(data => {
          if (data) {
            window.location.assign(data.url);
            if (code) {
              dispatch(authRedirectSucceeded(code));
              authToken(code)(dispatch);
            }
          }
        }).catch(error => {
          console.error(`failed to login user. error = ${JSON.stringify(error)}`);
          dispatch(authRedirectFailed(error));
          window.location.href = '/Logout.html';
        });
      }
    } else {
      dispatch(authRedirectSucceeded(code));
      authToken(code)(dispatch);
    }
  };
};

const authToken = (code) => (dispatch) => {
  let params = {
    code: code,
    redirectURI: window.location.origin
  };

  fetch('/api/auth/token', {
    method: 'POST',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(params)
  }).then(response => {
    if (response.ok) {
      return response.json();
    } else {
      setTimeout(() => window.location.href = window.location.origin, 3000);
    }
  }).then(data => {
    if (!!data && !!data.access_token) {
      saveTokensLocally(data.access_token, data.expires_in, data.id_token);
      authUserInfo(data.id_token, data.access_token)(dispatch);
      dispatch(authLoginSucceeded(data.access_token, data.expires_in));
      window.history.replaceState({}, document.title, "/");
      var redirectURI = sessionStorage.getItem('redirectURI');
      window.location.href = '/' + (redirectURI == null ? '#/' : redirectURI);
      sessionStorage.clear();
    } else {
      throw new Error("Unknown data received");
    }
  }).catch(error => {
    console.error(`failed to secure access token from okta. error = ${JSON.stringify(error)}`);
    dispatch(authLoginFailed(error));
    dispatch(authAuthorizationReset());
  });
};

export const authLogout = (accessToken) => (dispatch) => {
  if (accessToken === undefined) {
    accessToken = localStorage.getItem(authTokenKey);
  }

  let params = {
    access_token: accessToken,
    redirect_uri: window.location.origin
  };

  fetch('/api/auth/logout', {
    method: 'POST',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(params)
  })
    .then(response => {
      if (response.ok) {
        return response.json();
      } else {
        removeTokensLocally();
        dispatch(authLogoutSucceeded());
        dispatch(authAuthorizationReset());
        window.location.href = '/Logout.html';
      }
    })
    .then(data => {
      if (data) {
        removeTokensLocally();
        dispatch(authLogoutSucceeded());
        dispatch(authAuthorizationReset());
        window.location.assign(data.url);
      }
    })
    .catch(error => {
      console.error(`failed to logout user. error = ${JSON.stringify(error)}`);
      dispatch(authLogoutFailed(error));
      dispatch(authAuthorizationReset());
      window.location.href = '/Logout.html';
    });
};

export const setupInactivityTimer = (accessToken) => (dispatch) => {
  let timeout = 30 * 60 * 1000;  // after 30 mins of inactivity

  var inactivityTimer;
  const resetTimer = (name) => () => {
    if (inactivityTimer) {
      clearTimeout(inactivityTimer);
    }

    inactivityTimer = setTimeout(() => {
      saveRedirectURI();
      authLogout(accessToken)(dispatch);
    }, timeout);
  };

  window.onload = resetTimer('window.onload');
  document.onload = resetTimer('document.onload');
  document.onmousemove = resetTimer('document.onmousemove');
  document.onmousedown = resetTimer('document.onmousedown'); // touchscreen presses
  document.ontouchstart = resetTimer('document.ontouchstart');
  document.onclick = resetTimer('document.onclick');    // touchpad clicks
  document.onscroll = resetTimer('document.onscroll');    // scrolling with arrow keys
  document.onkeypress = resetTimer('document.onkeypress');
};

export const getLocalAuthToken = () => {
  var token = localStorage.getItem(authTokenKey);
  var expDate = localStorage.getItem(authExpirationDateKey);

  if (token && expDate) {
    var expDateParsed = Date.parse(expDate);
    if (expDateParsed < Date.now())
      return null;
    return token;
  }
  return null;
};

export const getLocalIdAuthToken = () => {
  var token = localStorage.getItem(idTokenKey);
  var expDate = localStorage.getItem(authExpirationDateKey);

  if (token && expDate) {
    var expDateParsed = Date.parse(expDate);
    if (expDateParsed < Date.now())
      return null;
    return token;
  }
  return null;
};

const saveTokensLocally = (authToken, authExp, idToken) => {
  var authExpire = new Date(Date.now());
  authExpire.setSeconds(authExpire.getSeconds() + authExp * 0.9);
  localStorage.setItem(authTokenKey, authToken);
  localStorage.setItem(idTokenKey, idToken);
  localStorage.setItem(authExpirationDateKey, authExpire.toISOString());
};

const removeTokensLocally = () => {
  localStorage.removeItem(authTokenKey);
  localStorage.removeItem(idTokenKey);
  localStorage.removeItem(authExpirationDateKey);
};

const authUserInfo = (idToken, accessToken) => (dispatch) => {
  let params = {
    idToken: idToken
  };

  dispatch(authAuthorizationPending());

  fetch('/api/auth/user', {
    method: 'POST',
    headers: {
      'Accept': 'application/json',
      'Content-Type': 'application/json',
      'Authorization': 'Bearer ' + accessToken
    },
    body: JSON.stringify(params)
  })
    .then(response => {
      if (response.ok) {
        return response.json();
      } else {
        removeTokensLocally();
        dispatch(authAuthorizationReset());
        window.location.href = '/Unauthorized.html';
      }
    })
    .then(data => {
        if (data) {
            const roles = data.roles;
            let userRolesSet = new Set(roles);
            let userRolesIntersection = [...USER_AUTHORIZED_ROLES_SET].filter(role => userRolesSet.has(role));
            if (userRolesIntersection.length > 0) {
                let primary_role;
                for (let item of userRolesSet) {
                    if (item == "dlx.conductor.admin") {
                        primary_role = "ADMIN";
                    } else if (item == "dlx.conductor.user") {
                        primary_role = "VIEWER";
                    }
                }
                dispatch(authAuthorizationSuccessful());
                dispatch(authInfoSucceeded(data.name, data.preferred_username, data.email, data.roles, primary_role));
            } else {
                removeTokensLocally();
                dispatch(authAuthorizationReset());
                window.location.href = '/Unauthorized.html';
            }
        } else {
            console.error(`Retreival of user info failed because there is no data returned`);
            removeTokensLocally();
            dispatch(authAuthorizationReset());
            window.location.href = '/Unauthorized.html';
        }
    })
    .catch(error => {
      console.error(`Retrieval of user info failed. error = ${error}`);
      removeTokensLocally();
      dispatch(authInfoFailed(error));
      dispatch(authAuthorizationError());
      dispatch(authAuthorizationReset());
      window.location.href= '/Unauthorized.html';
    });
};

