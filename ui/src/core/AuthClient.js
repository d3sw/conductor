import axios from 'axios';
import { v4 } from 'uuid';
import KJUR from 'jsrsasign';
import DnsResolver from './DnsResolver';
import {serverConfig} from './ServerConfig';

const oktaServiceUrl = serverConfig.oktaServiceUrl();
const oktaAuthServerCode = serverConfig.oktaAuthServerCode();

const client_id = serverConfig.clientId();
const client_secret = serverConfig.clientSecret();

const AuthClient = {

  getEncodedClientDetails () {
      // encode client details using base64
      let buff = Buffer.from(`${client_id}:${client_secret}`, 'utf-8');
      return buff.toString('base64');
  },

  async revokeToken(token, tokenType) {
    const params = {
      'token': token,
      'token_type_hint': tokenType
    };

    let encodedStr = this.getEncodedClientDetails();

    const body = Object.keys(params).map((key) => {
      return key + '=' + params[key];
    }).join('&');

    const config = {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Accept': 'application/json',
        'Authorization': `Basic ${encodedStr}`,
        'Cache-Control': 'no-cache'
      }
    };

    try {
        const authServiceUrl = `${oktaServiceUrl}/oauth2/${oktaAuthServerCode}/v1/revoke`;
        const res = await axios.post(authServiceUrl, body, config);
        return { status: true, data: res };
    } catch (error) {
        console.error(`failed to revoke ${tokenType}: status = ${error.response.status}, response = ${error.response.data}`);
        return { status: false, error: error };
    }
  },

  isValidIdToken(tokenPayload) {
    let currentDate = Date.now();
    let tokenExpirationDate = new Date(tokenPayload.exp * 1000);
    if (tokenExpirationDate < currentDate) {
      let error = `The id_token for this session has expired: ${tokenExpirationDate}`;
      console.error(error)
      return { status: false, error: error };
    }
      
    let oktaUrl = `${oktaServiceUrl}/oauth2/${oktaAuthServerCode}`;
    if (tokenPayload.iss !== oktaUrl) {
      let error = `Invalid issuer detected: ${tokenPayload.iss}`;
      console.error(error)
      return { status: false, error: error };
    }
    return { status: true };
  },

  resolveReqServiceHost(serviceName, success, error) {
    const host = serviceName;
    new DnsResolver().resolve(host, results => {
      if (results && results.length > 0)
        success(results[0]);
      else
        error(`Dns lookup failed for host ${host}. No matches found.`);
    }, err => error(err));
  },

  // gets an auth token
  login(redirectURI, success, error) {
    let config = {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    };

    let stateData = `state-${v4()}`;
    let scope = 'openid email profile';
    let oktaLoginUrl = `/oauth2/${oktaAuthServerCode}/v1/authorize?client_id=${client_id}&response_type=code&scope=${encodeURIComponent(scope)}&redirect_uri=${encodeURIComponent(redirectURI)}&state=${stateData}`;
    let oktaUrl = oktaServiceUrl + oktaLoginUrl;

    axios.get(oktaUrl, config)
      .then(response => {
        success(oktaUrl);
      }).catch(err => {
      error(err);
    });
  },

  // gets an auth token
  token(code, redirectURI, success, error) {
    const params = {
      'code': code,
      'grant_type': 'authorization_code',
      'redirect_uri': encodeURIComponent(redirectURI)
    };

    let encodedStr = this.getEncodedClientDetails();

    const body = Object.keys(params).map((key) => {
      return key + '=' + params[key];
    }).join('&');

    const config = {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Accept': 'application/json',
        'Authorization': `Basic ${encodedStr}`
      }
    };

    const authServiceUrl = `${oktaServiceUrl}/oauth2/${oktaAuthServerCode}/v1/token`;
    axios.post(authServiceUrl, body, config)
      .then(response => {
        success(response.data);
      }).catch(err => {
      error(err);
    });
  },

  logout(accessToken, redirectUri, success, error) {
    // invalidate the token
    this.revokeToken(accessToken, 'access_token').then(response => {
      if (!response.status) {
        error(response.error);
        return;
      }

      // sign the user out
      const logoutUrl = `${oktaServiceUrl}/login/signout?fromURI=${encodeURIComponent(redirectUri)}`;
      success(logoutUrl);
    });
  },

  user(token, success, error) {
    if (token === 'undefined') {
        error({ status: 400, data: `unable to retrieve user details because of invalid token [${token}]` });
        return;
    }

    var tokenPayload = token.split('.')[1];
    var payload = JSON.parse(KJUR.b64toutf8(tokenPayload));
    let validationStatus = this.isValidIdToken(payload);
    if (!validationStatus.status) {
      error({ status: 400, data: validationStatus.error });
      return;
    }

    let result = {
      name: payload["name"],
      email: payload["email"],
      expiration: new Date(),
      roles: payload['groups']
    }
    success(result);
  }
};

export default AuthClient;
