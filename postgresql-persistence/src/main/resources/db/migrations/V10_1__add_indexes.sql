CREATE INDEX IF NOT EXISTS queue_message_popped ON queue_message (popped);

CREATE INDEX IF NOT EXISTS queue_message_version ON queue_message (version);

CREATE INDEX IF NOT EXISTS queue_message_queue_name ON queue_message (queue_name);
