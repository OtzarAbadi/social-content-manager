-- Manual-review migration for MySQL 8. Do not run automatically.
-- Adds only the archive flag and leaves all existing client/content relationships unchanged.

SET @archive_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'clients'
      AND COLUMN_NAME = 'archived'
);

SET @archive_ddl = IF(
    @archive_column_exists = 0,
    'ALTER TABLE clients ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE AFTER phone',
    'SELECT ''clients.archived already exists'' AS migration_status'
);

PREPARE archive_statement FROM @archive_ddl;
EXECUTE archive_statement;
DEALLOCATE PREPARE archive_statement;

UPDATE clients SET archived = FALSE WHERE archived IS NULL;
