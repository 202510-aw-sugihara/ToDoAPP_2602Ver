-- 02_data.sql
-- Canonical Docker/PostgreSQL seed data script.

INSERT INTO categories (name, color)
SELECT 'Work', '#0d6efd'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Work');

INSERT INTO categories (name, color)
SELECT 'Private', '#198754'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Private');

INSERT INTO categories (name, color)
SELECT 'Urgent', '#dc3545'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Urgent');

INSERT INTO categories (name, color)
SELECT 'Other', '#fd7e14'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Other');

INSERT INTO users (username, password, roles, email, enabled)
SELECT 'user', '{noop}password', 'ROLE_USER', 'user@example.com', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user');

INSERT INTO users (username, password, roles, email, enabled)
SELECT 'admin', '{noop}adminpass', 'ROLE_ADMIN', 'admin@example.com', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO groups (name, type, parent_id, color)
SELECT '個人', 'PROJECT', NULL, '#6c757d'
WHERE NOT EXISTS (SELECT 1 FROM groups WHERE name = '個人' AND type = 'PROJECT');
