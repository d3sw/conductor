INSERT INTO META_PRIORITY (MIN_PRIORITY,MAX_PRIORITY,NAME,VALUE) VALUES
	 (1,10,'one-cdn-nomad-with-wtrmrk','one-cdn-packager-high'),
	 (1,2,'one-cdn-nomad-no-wtrmrk','one-cdn-packager-high'),
	 (3,10,'one-cdn-nomad-no-wtrmrk','one-cdn-packager')
	 on conflict do nothing;


INSERT INTO META_PRIORITY (MIN_PRIORITY,MAX_PRIORITY,NAME,VALUE) VALUES
	 (1,2,'one-cdn-batch-with-wtmrk','urgent'),
	 (3,10,'one-cdn-batch-with-wtmrk','high'),
	 (1,2,'one-cdn-batch-no-wtmrk','urgent'),
	 (3,4,'one-cdn-batch-no-wtmrk','high'),
	 (5,7,'one-cdn-batch-no-wtmrk','normal'),
	 (8,10,'one-cdn-batch-no-wtmrk','low')
	 on conflict do nothing;
