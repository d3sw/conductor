INSERT INTO META_PRIORITY (CREATED_ON,MODIFIED_ON,MIN_PRIORITY,MAX_PRIORITY,name,VALUE) VALUES
	 ('2023-01-30 11:22:23.990','2023-01-30 11:22:23.990',1,2,'vfs-server','urgent'),
	 ('2023-01-30 11:22:23.990','2023-01-30 11:22:23.990',3,4,'vfs-server','high'),
	 ('2023-01-30 11:22:23.990','2023-01-30 11:22:23.990',6,8,'vfs-server','normal'),
	 ('2023-01-30 11:22:23.990','2023-01-30 11:22:23.990',9,10,'vfs-server','low'),
	on conflict do nothing;
