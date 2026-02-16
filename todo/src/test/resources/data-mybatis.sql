INSERT INTO categories (id, name, color)
VALUES (1, 'Work', '#000000');

-- Aligns with current domain: status + deleted_at soft delete behavior.
INSERT INTO todos (id, author, title, description, due_date, priority, status, deleted_at,
  created_at, updated_at, version, user_id, category_id)
VALUES (1, 'Alice', 'Visible task', 'active row', '2026-02-06', 'MEDIUM', 'IN_PROGRESS', NULL,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 1, 1);

INSERT INTO todos (id, author, title, description, due_date, priority, status, deleted_at,
  created_at, updated_at, version, user_id, category_id)
VALUES (2, 'Bob', 'Deleted task', 'soft deleted row', '2026-02-07', 'HIGH', 'COMPLETED',
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 1, 1);

INSERT INTO todos (id, author, title, description, due_date, priority, status, deleted_at,
  created_at, updated_at, version, user_id, category_id)
VALUES (3, 'Carol', 'Other user task', 'different owner', '2026-02-08', 'LOW', 'PLANNED', NULL,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 2, 1);