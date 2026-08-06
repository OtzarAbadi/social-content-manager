CREATE TABLE IF NOT EXISTS content_media (
  media_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  content_id INT NOT NULL,
  media_url VARCHAR(2048) NOT NULL,
  media_type VARCHAR(20) NOT NULL,
  display_order INT NOT NULL,
  thumbnail_url VARCHAR(2048) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS content_version_media (
  version_media_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  content_version_id BIGINT NOT NULL,
  media_url VARCHAR(2048) NOT NULL,
  media_type VARCHAR(20) NOT NULL,
  display_order INT NOT NULL,
  thumbnail_url VARCHAR(2048) NULL
);

SET @schema_name=DATABASE();
SET @sql=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='content_media' AND index_name='uk_content_media_order'),'SELECT 1','CREATE UNIQUE INDEX uk_content_media_order ON content_media(content_id,display_order)'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='content_media' AND index_name='idx_content_media_content'),'SELECT 1','CREATE INDEX idx_content_media_content ON content_media(content_id)'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql=IF(EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=@schema_name AND table_name='content_media' AND constraint_name='fk_content_media_content'),'SELECT 1','ALTER TABLE content_media ADD CONSTRAINT fk_content_media_content FOREIGN KEY(content_id) REFERENCES contents(content_id)'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='content_version_media' AND index_name='uk_version_media_order'),'SELECT 1','CREATE UNIQUE INDEX uk_version_media_order ON content_version_media(content_version_id,display_order)'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql=IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=@schema_name AND table_name='content_version_media' AND index_name='idx_version_media_version'),'SELECT 1','CREATE INDEX idx_version_media_version ON content_version_media(content_version_id)'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql=IF(EXISTS(SELECT 1 FROM information_schema.table_constraints WHERE constraint_schema=@schema_name AND table_name='content_version_media' AND constraint_name='fk_version_media_version'),'SELECT 1','ALTER TABLE content_version_media ADD CONSTRAINT fk_version_media_version FOREIGN KEY(content_version_id) REFERENCES content_versions(content_version_id)'); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
