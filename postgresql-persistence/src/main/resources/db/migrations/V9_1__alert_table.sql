CREATE TABLE IF NOT EXISTS alert_registry
(
    id                    bigserial   NOT NULL,
    lookup                TEXT        NOT NULL,
    general_message       TEXT,
    alert_count           int4,
    CONSTRAINT alert_registry_pkey PRIMARY KEY (id)
);

CREATE UNIQUE INDEX alert_registry_lookup_uq ON alert_registry (lookup);

CREATE TABLE IF NOT EXISTS alerts (
    id bigserial NOT NULL,
    timestamp TIMESTAMP DEFAULT NOW(),
    message TEXT,
    alert_lookup_id int4 NULL,
    is_processed boolean default false not null,
    CONSTRAINT alerts_pkey PRIMARY KEY (id)
);

insert into alert_registry (lookup,general_message,alert_count)
values('event handler condition validation failed','Event Handler Validation Failed on Startup','1')
ON CONFLICT DO NOTHING;

insert into alert_registry (lookup,general_message,alert_count)
values('Workflow might have a stuck state. workflowId','Workflow has a Stuck State Warning','1')
ON CONFLICT DO NOTHING;

insert into alert_registry (lookup,general_message,alert_count)
values('Workflow is still running. status=RUNNING  Timeout/fail/reset error occurred','Timeout/fail/reset error occurred','1')
ON CONFLICT DO NOTHING;

insert into alert_registry (lookup,general_message,alert_count)
values('No task found with reference name','Workflow Wait task Warning','1')
ON CONFLICT DO NOTHING;

insert into alert_registry (lookup,general_message,alert_count)
values('ONECOND-1106','ONECOND-1106 empty/null workflowID','1')
ON CONFLICT DO NOTHING;

insert into alert_registry (lookup,general_message,alert_count)
values('Service discovery failed','Service discovery failed','1')
ON CONFLICT DO NOTHING;