import {Router} from 'express';
import http from '../core/HttpClient';
import lookup from '../core/ApiLookup';

const router = new Router();

router.get('/', async (req, res, next) => {

  const server = await lookup.lookup();
  try {
    const result = {
      server: server,
      env: process.env
    };
    const config = await http.get(server + 'admin/config');
    result.version = config.version;
    result.buildDate = config.buildDate;
    res.status(200).send({sys: result});
  } catch (err) {
    next(err);
  }
});

router.get('/versions', async (req, res, next) => {

  const server = await lookup.lookup();
  try {
    const result = {
      server: server,
      env: process.env
    };
    const config = await http.get(server + 'v1/status');
    result.version = config.conductor_core_version;
    result.initializerVersion = config.conductor_initializer_version;
    result.composerVersion = config.workflow_composer_version;
    result.buildDate = config.buildDate;
    res.status(200).send({sys: result});
  } catch (err) {
    next(err);
  }
});
module.exports = router;
