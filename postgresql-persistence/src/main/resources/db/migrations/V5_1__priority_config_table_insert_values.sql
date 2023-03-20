INSERT INTO META_PRIORITY (MIN_PRIORITY,MAX_PRIORITY,name,VALUE) VALUES
	 (1,1,'hls-nexguard-server','hybrik-hls-proxy-lightning'),
	 (2,2,'hls-nexguard-server','hybrik-hls-proxy-urgent'),
	 (3,4,'hls-nexguard-server','hybrik-hls-proxy-high'),
	 (5,6,'hls-nexguard-server','hybrik-hls-proxy-medium'),
	 (7,8,'hls-nexguard-server','hybrik-hls-proxy'),
	 (9,10,'hls-nexguard-server','hybrik-hls-proxy-low')
	on conflict do nothing;

INSERT INTO META_PRIORITY (MIN_PRIORITY,MAX_PRIORITY,name,VALUE) VALUES
	 (1,1,'hls-nielsen-server','hybrik-hls-proxy-lightning'),
	 (2,2,'hls-nielsen-server','hybrik-hls-proxy-urgent'),
	 (3,4,'hls-nielsen-server','hybrik-hls-proxy-high'),
	 (5,6,'hls-nielsen-server','hybrik-hls-proxy-medium'),
	 (7,8,'hls-nielsen-server','hybrik-hls-proxy'),
	 (9,10,'hls-nielsen-server','hybrik-hls-proxy-low')
	on conflict do nothing;

INSERT INTO META_PRIORITY (MIN_PRIORITY,MAX_PRIORITY,name,VALUE) VALUES
	 (1,1,'hls-packaging-server','hybrik-hls-proxy-lightning'),
	 (2,2,'hls-packaging-server','hybrik-hls-proxy-urgent'),
	 (3,4,'hls-packaging-server','hybrik-hls-proxy-high'),
	 (5,6,'hls-packaging-server','hybrik-hls-proxy-medium'),
	 (7,8,'hls-packaging-server','hybrik-hls-proxy'),
	 (9,10,'hls-packaging-server','hybrik-hls-proxy-low')
	on conflict do nothing;
