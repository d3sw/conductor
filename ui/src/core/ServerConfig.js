export const serverConfig = {
  port() { return process.env.PORT || 5000; },

  oktaServiceUrl() {
    return process.env.OKTA_SERVICE_URL;
  },

  oktaAuthServerCode() {
    return process.env.OKTA_AUTH_SERVER_CODE;
  },

  clientId() {
    return process.env.CLIENT_ID
  },

  clientSecret() {
    return process.env.CLIENT_SECRET;
  }
};