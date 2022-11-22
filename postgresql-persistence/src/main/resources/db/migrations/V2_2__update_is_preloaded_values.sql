UPDATE meta_config
SET is_preloaded = true
WHERE name = 'cc_extract_server';

UPDATE meta_config
SET is_preloaded = true
WHERE name = 'one-cdn_server';

UPDATE meta_config
SET is_preloaded = true
WHERE name = 'checksum_server';