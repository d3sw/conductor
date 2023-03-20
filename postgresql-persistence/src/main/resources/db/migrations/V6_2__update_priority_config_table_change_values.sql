-- change medium to normal for ccextract
UPDATE meta_priority SET value = 'normal' WHERE name = "ccextract" AND value = 'medium';

-- change medium to normal for checksum
UPDATE meta_priority SET value = 'normal' WHERE name = "checksum" AND value = 'medium';

-- change medium to normal for one-cdn-packager
UPDATE meta_priority SET value = 'normal' WHERE name = "one-cdn-packager" AND value = 'medium';
