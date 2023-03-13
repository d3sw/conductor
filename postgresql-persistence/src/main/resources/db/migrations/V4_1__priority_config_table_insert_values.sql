CREATE UNIQUE INDEX IF NOT EXISTS meta_priority_uniq_idx ON META_PRIORITY(min_priority, max_priority, name);

INSERT INTO META_PRIORITY (MIN_PRIORITY, MAX_PRIORITY, name, value) VALUES
	 (1,2,'vfs-server','urgent'),
	 (3,4,'vfs-server','high'),
	 (5,8,'vfs-server','normal'),
	 (9,10,'vfs-server','low')
	on conflict do nothing;

DROP TABLE IF EXISTS CONFIG_STORE;
