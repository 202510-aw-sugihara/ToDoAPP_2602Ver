-- 01_schema.sql
-- Canonical Docker/PostgreSQL schema initialization script.

CREATE TABLE IF NOT EXISTS categories (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  color VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(200) NOT NULL,
  roles VARCHAR(200) NOT NULL,
  email VARCHAR(200) NOT NULL,
  enabled BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS groups (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20) NOT NULL,
  parent_id BIGINT NULL,
  color VARCHAR(20) NOT NULL
);

ALTER TABLE groups
  ADD CONSTRAINT IF NOT EXISTS fk_groups_parent
  FOREIGN KEY (parent_id) REFERENCES groups(id);

CREATE TABLE IF NOT EXISTS todos (
  id BIGSERIAL PRIMARY KEY,
  author VARCHAR(50) NOT NULL,
  title VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  due_date DATE,
  priority VARCHAR(10) NOT NULL,
  category_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20),
  deleted_at TIMESTAMP,
  version BIGINT,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);

ALTER TABLE todos
  ADD CONSTRAINT IF NOT EXISTS fk_todos_category
  FOREIGN KEY (category_id) REFERENCES categories(id);

ALTER TABLE todos
  ADD CONSTRAINT IF NOT EXISTS fk_todos_user
  FOREIGN KEY (user_id) REFERENCES users(id);

CREATE TABLE IF NOT EXISTS todo_groups (
  todo_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  PRIMARY KEY (todo_id, group_id)
);

ALTER TABLE todo_groups
  ADD CONSTRAINT IF NOT EXISTS fk_todo_groups_todo
  FOREIGN KEY (todo_id) REFERENCES todos(id);

ALTER TABLE todo_groups
  ADD CONSTRAINT IF NOT EXISTS fk_todo_groups_group
  FOREIGN KEY (group_id) REFERENCES groups(id);

CREATE TABLE IF NOT EXISTS user_groups (
  user_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, group_id)
);

ALTER TABLE user_groups
  ADD CONSTRAINT IF NOT EXISTS fk_user_groups_user
  FOREIGN KEY (user_id) REFERENCES users(id);

ALTER TABLE user_groups
  ADD CONSTRAINT IF NOT EXISTS fk_user_groups_group
  FOREIGN KEY (group_id) REFERENCES groups(id);

CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGSERIAL PRIMARY KEY,
  action VARCHAR(100) NOT NULL,
  username VARCHAR(100),
  target_type VARCHAR(100),
  target_id VARCHAR(100),
  detail VARCHAR(1000),
  before_value VARCHAR(4000),
  after_value VARCHAR(4000),
  created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at
  ON audit_logs (created_at);

CREATE TABLE IF NOT EXISTS todo_attachments (
  id BIGSERIAL PRIMARY KEY,
  todo_id BIGINT NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  stored_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(100),
  size BIGINT NOT NULL,
  uploaded_at TIMESTAMP NOT NULL
);

ALTER TABLE todo_attachments
  ADD CONSTRAINT IF NOT EXISTS fk_todo_attachments_todo
  FOREIGN KEY (todo_id) REFERENCES todos(id);
