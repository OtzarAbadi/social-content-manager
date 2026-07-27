USE social_content_manager;

ALTER TABLE notifications
    ADD COLUMN related_entity_id INT NULL AFTER related_content_id;

UPDATE notifications
SET related_entity_id = related_content_id
WHERE related_entity_id IS NULL;
