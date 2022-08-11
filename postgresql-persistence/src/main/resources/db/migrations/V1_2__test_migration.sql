CREATE INDEX workflow_json_data_workflow_ids_gin_idx
    ON workflow USING gin (json_data_workflow_ids jsonb_path_ops);

CREATE INDEX IF NOT EXISTS workflow_test
    ON ghost_table;