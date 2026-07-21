USE social_content_manager;

CREATE TABLE IF NOT EXISTS content_versions (
    content_version_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content_id INT NOT NULL,
    version_number INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    content_type ENUM('IMAGE', 'VIDEO', 'TEXT') NOT NULL,
    file_url VARCHAR(500),
    status ENUM('DRAFT', 'WAITING_APPROVAL', 'APPROVED', 'REJECTED', 'PUBLISHED') NOT NULL,
    planned_publish_date DATETIME,
    changed_by_user_id INT NULL,
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_type ENUM('CREATED', 'EDITED', 'SCHEDULED', 'STATUS_CHANGED') NOT NULL,

    CONSTRAINT uk_content_version_number UNIQUE (content_id, version_number),
    CONSTRAINT fk_content_versions_content
        FOREIGN KEY (content_id) REFERENCES contents(content_id) ON DELETE CASCADE,
    CONSTRAINT fk_content_versions_changed_by
        FOREIGN KEY (changed_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_content_versions_history (content_id, version_number)
);
