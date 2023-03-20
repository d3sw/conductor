-- ccextract configuration
INSERT INTO META_PRIORITY (MIN_PRIORITY,MAX_PRIORITY,name,VALUE) VALUES
	 (1,2,'ccextract','urgent'),
	 (3,4,'ccextract','high'),
	 (5,7,'ccextract','medium'),
	 (8,10,'ccextract','low')
	on conflict do nothing;

-- checksum configuration
INSERT INTO META_PRIORITY (MIN_PRIORITY,MAX_PRIORITY,name,VALUE) VALUES
	 (1,2,'checksum','urgent'),
	 (3,4,'checksum','high'),
	 (5,7,'checksum','medium'),
	 (8,10,'checksum','low')
	on conflict do nothing;

-- ONE CDN configuration
INSERT INTO META_PRIORITY (MIN_PRIORITY,MAX_PRIORITY,name,VALUE) VALUES
	 (1,2,'one-cdn-packager','urgent'),
	 (3,4,'one-cdn-packager','high'),
	 (5,7,'one-cdn-packager','medium'),
	 (8,10,'one-cdn-packager','low')
	on conflict do nothing;
