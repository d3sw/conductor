import {Router} from 'express';
import log4js from 'log4js';
import authClient from '../core/AuthClient';

const logger = log4js.getLogger('server.routes.auth');

const router = new Router();

router.post('/auth/login', (req, res) => {
  authClient.login(req.body.redirectURI, data => {
    res.send({url: data});
  }, error => {
    logger.error(`in route /auth/login error: ${error}`);
    res.status(error.response.status).send(error.response.data);
  });
});

router.post('/auth/token', (req, res) => {
  authClient.token(req.body.code, req.body.redirectURI, data => {
    res.send(data);
  }, error => {
    logger.error(`in route /auth/token error: ${error}:${JSON.stringify(error.response.data)}`);
    res.status(error.response.status).send(error.response.data);
  });
});

router.post('/auth/logout', (req, res) => {
  authClient.logout(req.body.access_token, req.body.redirect_uri, data => {
    res.send({url: data});
  }, error => {
    logger.error(`in route /auth/logout error: ${error}`);
    res.status(error.response.status).send(error.response.data);
  });
});

router.post('/auth/user', (req, res) => {
  authClient.user(req.body.idToken, data => {
    res.json(data);
  }, error => {
    logger.error(`in route /auth/user error: ${JSON.stringify(error)}`);
    res.status(error.status).send(error.data);
  });
});

module.exports = router;