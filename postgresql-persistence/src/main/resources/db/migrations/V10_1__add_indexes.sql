CREATE INDEX IF NOT EXISTS queue_message_popped ON queue_message (popped);

CREATE INDEX IF NOT EXISTS queue_message_version ON queue_message (version);

CREATE INDEX IF NOT EXISTS queue_message_queue_name ON queue_message (queue_name);

CREATE INDEX IF NOT EXISTS queue_message_message_id ON queue_message (message_id);

CREATE INDEX IF NOT EXISTS queue_message_queue_name_message_id ON queue_message (queue_name, message_id);

CREATE INDEX IF NOT EXISTS queue_message_queue_name_deliver_on_popped ON queue_message (queue_name, deliver_on, popped);

CREATE INDEX IF NOT EXISTS workflow_error_registry_workflow_id ON workflow_error_registry (workflow_id);

CREATE INDEX IF NOT EXISTS task_in_progress_task_id ON task_in_progress (task_id);

CREATE INDEX IF NOT EXISTS event_execution_execution_id ON event_execution (execution_id);

CREATE INDEX IF NOT EXISTS task_task_refname ON task (task_refname);

CREATE INDEX IF NOT EXISTS task_workflow_id ON task (workflow_id);

CREATE INDEX IF NOT EXISTS task_task_id ON task (task_id);

CREATE INDEX IF NOT EXISTS workflow_workflow_id ON workflow (workflow_id);

CREATE INDEX IF NOT EXISTS workflow_workflow_id_workflow_type ON workflow (workflow_id, workflow_type);
