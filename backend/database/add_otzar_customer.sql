-- Separates the administrator identity from the customer that owns Instagram.
UPDATE users
SET full_name = 'Admin',
    password = '$2a$10$ruYktCywZWcCmcdsm8xMQe/4G1jOU/e9rtO7j4qKyV9iQ4EHiwf9K'
WHERE username = 'admin' AND role = 'ADMIN';

INSERT INTO users (full_name, email, username, password, role, token)
SELECT 'Otzar', 'otzar@sscm.com', 'otzar',
       '$2a$10$QiC290Pfu8DVrzOm3GaAkOZLnoBYzH5ogIZG8Uzp3HLgU9D4D2.fm', 'CLIENT', ''
FROM users admin_user
WHERE admin_user.role = 'ADMIN'
  AND NOT EXISTS (SELECT 1 FROM users WHERE username = 'otzar')
LIMIT 1;

UPDATE users
SET password = '$2a$10$QiC290Pfu8DVrzOm3GaAkOZLnoBYzH5ogIZG8Uzp3HLgU9D4D2.fm'
WHERE username = 'otzar' AND role = 'CLIENT';

INSERT INTO clients (user_id, admin_id, business_name, phone)
SELECT otzar_user.user_id, admin_record.admin_id, 'Otzar', ''
FROM users otzar_user
CROSS JOIN admins admin_record
WHERE otzar_user.username = 'otzar'
  AND NOT EXISTS (SELECT 1 FROM clients WHERE business_name = 'Otzar')
LIMIT 1;
