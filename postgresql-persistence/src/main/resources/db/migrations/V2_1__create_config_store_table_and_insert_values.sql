create table if not exists config_store
(
    name  varchar(255) not null
        primary key,
    value text,
    is_preloaded boolean default false not null
);


INSERT INTO config_store (name, value, is_preloaded) VALUES ('cc_extract_server', 'http://nomad.service.${TLD}:4646', true) ON CONFLICT DO NOTHING;
INSERT INTO config_store (name, value, is_preloaded) VALUES ('checksum_server', 'http://nomad.service.${TLD}:4646', true) ON CONFLICT DO NOTHING;
INSERT INTO config_store (name, value, is_preloaded) VALUES ('one-cdn_server', 'http://nomad.service.${TLD}:4646', true) ON CONFLICT DO NOTHING;