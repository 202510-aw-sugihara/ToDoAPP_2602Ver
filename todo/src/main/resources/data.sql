insert into categories (name, color) values ('仕事', '#0d6efd');
insert into categories (name, color) values ('プライベート', '#198754');
insert into categories (name, color) values ('緊急', '#dc3545');
insert into categories (name, color) values ('その他', '#fd7e14');

insert into users (username, password, roles, email, enabled) values ('user', '{noop}password', 'ROLE_USER', 'user@example.com', true);
insert into users (username, password, roles, email, enabled) values ('admin', '{noop}adminpass', 'ROLE_ADMIN', 'admin@example.com', true);

insert into todos (author, title, description, due_date, priority, category_id, user_id, status, created_at, updated_at)
select 'user', '月次レポート提出', '経費集計とレポート送付', DATEADD('DAY', -1, CURRENT_DATE),
  'HIGH', 1, u.id, 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from users u where u.username = 'user';

insert into todos (author, title, description, due_date, priority, category_id, user_id, status, created_at, updated_at)
select 'user', 'ジムの登録更新', '更新手続きとプラン確認', DATEADD('DAY', 7, CURRENT_DATE),
  'MEDIUM', 2, u.id, 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from users u where u.username = 'user';

insert into todos (author, title, description, due_date, priority, category_id, user_id, status, created_at, updated_at)
select 'user', '障害連絡の整理', '緊急連絡網と対応フローの見直し', DATEADD('DAY', 1, CURRENT_DATE),
  'HIGH', 3, u.id, 'ON_HOLD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from users u where u.username = 'user';

insert into todos (author, title, description, due_date, priority, category_id, user_id, status, created_at, updated_at)
select 'user', '読書リスト整理', '興味分野ごとにメモを作る', DATEADD('DAY', 14, CURRENT_DATE),
  'LOW', 4, u.id, 'PLANNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from users u where u.username = 'user';

insert into todos (author, title, description, due_date, priority, category_id, user_id, status, created_at, updated_at)
select 'admin', '監査ログの月次確認', '監査レポートをまとめて共有', DATEADD('DAY', 3, CURRENT_DATE),
  'MEDIUM', 1, u.id, 'COMPLETED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from users u where u.username = 'admin';

insert into todos (author, title, description, due_date, priority, category_id, user_id, status, created_at, updated_at)
select 'admin', '家計の支出チェック', '今月の支出カテゴリを整理', DATEADD('DAY', 6, CURRENT_DATE),
  'LOW', 2, u.id, 'IN_PROGRESS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from users u where u.username = 'admin';

insert into todos (author, title, description, due_date, priority, category_id, user_id, status, created_at, updated_at)
select 'admin', '緊急連絡先の更新', '関係者の連絡先を最新化', DATEADD('DAY', 1, CURRENT_DATE),
  'HIGH', 3, u.id, 'ON_HOLD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from users u where u.username = 'admin';

insert into todos (author, title, description, due_date, priority, category_id, user_id, status, created_at, updated_at)
select 'admin', '備品棚卸し計画', '棚卸しの手順と担当を整理', DATEADD('DAY', 10, CURRENT_DATE),
  'MEDIUM', 4, u.id, 'PLANNED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
from users u where u.username = 'admin';

insert into groups (name, type, parent_id, color) values ('Company A', 'COMPANY', null, '#0d6efd');
insert into groups (name, type, parent_id, color) values ('Company B', 'COMPANY', null, '#198754');

insert into groups (name, type, parent_id, color)
select 'Sales', 'DEPARTMENT', id, '#0d6efd' from groups where name = 'Company A' and type = 'COMPANY';
insert into groups (name, type, parent_id, color)
select 'Development', 'DEPARTMENT', id, '#0d6efd' from groups where name = 'Company A' and type = 'COMPANY';
insert into groups (name, type, parent_id, color)
select 'Support', 'DEPARTMENT', id, '#198754' from groups where name = 'Company B' and type = 'COMPANY';

insert into groups (name, type, parent_id, color)
select 'Project Apollo', 'PROJECT', id, '#0d6efd' from groups where name = 'Sales' and type = 'DEPARTMENT';
insert into groups (name, type, parent_id, color)
select 'Project Orion', 'PROJECT', id, '#198754' from groups where name = 'Development' and type = 'DEPARTMENT';

insert into groups (name, type, parent_id, color) values ('Client X', 'CLIENT', null, '#dc3545');
insert into groups (name, type, parent_id, color) values ('Client Y', 'CLIENT', null, '#fd7e14');

insert into groups (name, type, parent_id, color)
select 'Project X-1', 'PROJECT', id, '#dc3545' from groups where name = 'Client X' and type = 'CLIENT';
insert into groups (name, type, parent_id, color)
select 'Project Y-1', 'PROJECT', id, '#fd7e14' from groups where name = 'Client Y' and type = 'CLIENT';

insert into groups (name, type, parent_id, color) values ('個人', 'PROJECT', null, '#6c757d');
