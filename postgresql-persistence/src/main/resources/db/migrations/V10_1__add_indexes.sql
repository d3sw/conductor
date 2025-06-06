CREATE INDEX IF NOT EXISTS queue_message_popped ON queue_message (popped);

CREATE INDEX IF NOT EXISTS queue_message_version ON queue_message (version);
