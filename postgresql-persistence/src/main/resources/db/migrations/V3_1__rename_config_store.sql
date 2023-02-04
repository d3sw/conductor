create table if not exists app_config
(
    key  varchar(255) not null primary key,
    value text
);


INSERT INTO app_config (key, value) VALUES ('cc_extract_server', 'http://nomad.service.${TLD}:4646') ON CONFLICT DO NOTHING;
INSERT INTO app_config (key, value) VALUES ('checksum_server', 'http://nomad.service.${TLD}:4646') ON CONFLICT DO NOTHING;
INSERT INTO app_config (key, value) VALUES ('one-cdn_server', 'http://nomad.service.${TLD}:4646') ON CONFLICT DO NOTHING;


