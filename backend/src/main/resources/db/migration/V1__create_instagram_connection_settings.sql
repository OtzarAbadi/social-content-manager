CREATE TABLE IF NOT EXISTS instagram_connection_settings (
    settings_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    instagram_user_id VARCHAR(40) NOT NULL,
    graph_api_base_url VARCHAR(255) NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
