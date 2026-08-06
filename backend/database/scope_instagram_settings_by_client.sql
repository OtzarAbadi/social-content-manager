-- Backup-safe, idempotent MySQL migration. Review and back up before running.
-- Existing settings are retained if username 'otzar' is not linked to exactly one client.
SET @schema_name = DATABASE();

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema=@schema_name AND table_name='instagram_connection_settings' AND column_name='client_id'),
  'SELECT 1',
  'ALTER TABLE instagram_connection_settings ADD COLUMN client_id INT NULL AFTER settings_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @otzar_client_id = (
  SELECT MIN(c.client_id)
  FROM clients c JOIN users u ON u.user_id = c.user_id
  WHERE LOWER(u.username) = 'otzar'
  HAVING COUNT(*) = 1
);

SET @existing_settings_id = (
  SELECT MIN(settings_id)
  FROM instagram_connection_settings
  WHERE client_id IS NULL
);

SET @otzar_already_assigned = (
  SELECT COUNT(*)
  FROM instagram_connection_settings
  WHERE client_id = @otzar_client_id
);

UPDATE instagram_connection_settings
SET client_id = @otzar_client_id
WHERE settings_id = @existing_settings_id
  AND client_id IS NULL
  AND @otzar_client_id IS NOT NULL
  AND @otzar_already_assigned = 0;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.statistics
         WHERE table_schema=@schema_name AND table_name='instagram_connection_settings' AND index_name='uk_instagram_settings_client'),
  'SELECT 1',
  'CREATE UNIQUE INDEX uk_instagram_settings_client ON instagram_connection_settings(client_id)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_schema=@schema_name AND table_name='instagram_connection_settings' AND constraint_name='fk_instagram_settings_client'),
  'SELECT 1',
  'ALTER TABLE instagram_connection_settings ADD CONSTRAINT fk_instagram_settings_client FOREIGN KEY (client_id) REFERENCES clients(client_id)'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
