INSERT INTO categories (id, name, color)
VALUES (1, 'Work', '#000000');

INSERT INTO todos (id, author, title, description, due_date, priority, completed,
  created_at, updated_at, version, user_id, category_id)
VALUES (1, 'Alice', 'First task', 'desc', '2026-02-06', 'MEDIUM', false,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 1, 1);

INSERT INTO todos (id, author, title, description, due_date, priority, completed,
  created_at, updated_at, version, user_id, category_id)
VALUES (2, 'Bob', 'Second task', 'desc2', '2026-02-07', 'HIGH', false,
  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 1, 1);
