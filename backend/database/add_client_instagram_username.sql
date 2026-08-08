-- Manual-review migration for MySQL 8. Do not run automatically.
-- Existing clients remain valid because the new field is nullable.

SET @instagram_username_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clients'
      AND COLUMN_NAME = 'instagram_username'
);

SET @instagram_username_ddl = IF(
    @instagram_username_column_exists = 0,
    'ALTER TABLE clients ADD COLUMN instagram_username VARCHAR(100) NULL AFTER phone',
    'SELECT ''clients.instagram_username already exists'' AS migration_status'
);

PREPARE instagram_username_statement FROM @instagram_username_ddl;
EXECUTE instagram_username_statement;
DEALLOCATE PREPARE instagram_username_statement;
