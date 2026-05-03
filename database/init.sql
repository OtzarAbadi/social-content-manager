DROP DATABASE IF EXISTS social_content_manager;
CREATE DATABASE social_content_manager
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE social_content_manager;

CREATE TABLE users (
                       user_id INT AUTO_INCREMENT PRIMARY KEY,
                       full_name VARCHAR(100) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role ENUM('ADMIN', 'CLIENT') NOT NULL,
                       created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE admins (
                        admin_id INT AUTO_INCREMENT PRIMARY KEY,
                        user_id INT NOT NULL UNIQUE,
                        can_manage_clients BOOLEAN DEFAULT TRUE,
                        can_publish_content BOOLEAN DEFAULT TRUE,
                        can_view_analytics BOOLEAN DEFAULT TRUE,
                        created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                        FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE clients (
                         client_id INT AUTO_INCREMENT PRIMARY KEY,
                         user_id INT NOT NULL,
                         admin_id INT,
                         business_name VARCHAR(150) NOT NULL,
                         phone VARCHAR(20),
                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                         FOREIGN KEY (user_id) REFERENCES users(user_id),
                         FOREIGN KEY (admin_id) REFERENCES admins(admin_id)
);

CREATE TABLE contents (
                          content_id INT AUTO_INCREMENT PRIMARY KEY,
                          client_id INT NOT NULL,
                          title VARCHAR(150) NOT NULL,
                          description TEXT,
                          file_url VARCHAR(500),
                          content_type ENUM('IMAGE', 'VIDEO', 'TEXT') NOT NULL,
                          status ENUM('DRAFT', 'WAITING_APPROVAL', 'APPROVED', 'REJECTED') DEFAULT 'DRAFT',
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                          FOREIGN KEY (client_id) REFERENCES clients(client_id)
);

CREATE TABLE comments (
                          comment_id INT AUTO_INCREMENT PRIMARY KEY,
                          content_id INT NOT NULL,
                          user_id INT NOT NULL,
                          comment_text TEXT NOT NULL,
                          created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                          FOREIGN KEY (content_id) REFERENCES contents(content_id),
                          FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE comments (
                          comment_id INT AUTO_INCREMENT PRIMARY KEY,
                          content_id INT,
                          user_id INT,
                          text VARCHAR(255),
                          FOREIGN KEY (content_id) REFERENCES contents(content_id),
                          FOREIGN KEY (user_id) REFERENCES users(user_id)
);

INSERT INTO users (full_name, email, password, role)
VALUES
    ('Otzar Admin', 'admin@sscm.com', '123456', 'ADMIN'),
    ('Stav Beauty Studio', 'client1@sscm.com', '123456', 'CLIENT'),
    ('Hodaya Nails', 'client2@sscm.com', '123456', 'CLIENT');

INSERT INTO admins (user_id)
VALUES (1);

INSERT INTO clients (user_id, admin_id, business_name, phone)
VALUES
    (2, 1, 'Stav Beauty Studio', '0501234567'),
    (3, 1, 'Hodaya Nails', '0527654321');

INSERT INTO contents (client_id, title, description, file_url, content_type, status)
VALUES
    (1, 'פוסט פתיחה לאינסטגרם', 'פוסט היכרות לעסק, מיועד לפרסום ביום ראשון בערב', 'https://example.com/post1.jpg', 'IMAGE', 'WAITING_APPROVAL'),
    (1, 'רילס לפני ואחרי', 'וידאו קצר המציג תוצאה של טיפול לפני ואחרי', 'https://example.com/reel1.mp4', 'VIDEO', 'DRAFT'),
    (2, 'מבצע לק ג׳ל', 'פוסט מבצע לחודש הקרוב עבור לק ג׳ל', 'https://example.com/post2.jpg', 'IMAGE', 'APPROVED');

INSERT INTO comments (content_id, user_id, comment_text)
VALUES
    (1, 2, 'אהבתי את העיצוב, אפשר רק לשנות את הטקסט בסוף?'),
    (2, 1, 'העליתי גרסה ראשונית של הרילס, ממתין לאישור.'),
    (3, 3, 'מאשרת, אפשר לפרסם.');