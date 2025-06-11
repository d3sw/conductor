CREATE INDEX IF NOT EXISTS meta_workflow_def_name_idx ON meta_workflow_def (name);

DROP INDEX IF EXISTS task_type_start;

CREATE INDEX IF NOT EXISTS task_type_time ON task (task_type, start_time);

