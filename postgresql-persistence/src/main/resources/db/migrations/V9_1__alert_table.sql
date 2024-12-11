CREATE TABLE IF NOT EXISTS alert_registry
(
    id                    bigserial   NOT NULL,
    lookup                TEXT        NOT NULL,
    general_message       TEXT,
    alert_count           int4,
    CONSTRAINT alert_registry_pkey PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS alerts (
    id bigserial NOT NULL,
    timestamp TIMESTAMP DEFAULT NOW(),
    message TEXT,
    alert_lookup_id int4 NULL,
    is_processed boolean default false not null,
    CONSTRAINT alerts_pkey PRIMARY KEY (id)
);