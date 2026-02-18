ToDo App (Spring Boot)

概要

本リポジトリは、Spring Boot 3.5 / Thymeleaf を用いた ToDo 管理アプリケーションです。
基本的なタスク管理機能に加え、認証/認可、グループ境界制御、添付ファイル管理、CSV 出力、メール通知、監査ログ、論理削除を実装しています。

実務利用を想定し、権限制御・監査・復元可能な削除設計・CIによる品質担保まで含めた構成としています。

対象ユーザー

個人または小規模チームで、期限・状態・カテゴリを使ってタスク管理したいユーザー

管理者としてユーザー管理、グループ管理、監査ログ確認、削除済み ToDo の復元・完全削除を行いたいユーザー

解決する課題

ToDo の作成・更新・検索・状態管理を一元化

ユーザー単位 + グループ単位で閲覧境界を制御

変更履歴を監査ログとして記録し、追跡可能性を確保

誤削除時に復元可能な論理削除設計

管理者による完全削除機能

主要機能

認証/認可（Spring Security）

ToDo CRUD（Web + REST API）

カテゴリ管理

グループ管理（部署・案件などの論理境界）

添付ファイルのアップロード/ダウンロード/プレビュー

CSV エクスポート

期限近接 ToDo のメール通知（Scheduler）

監査ログの記録/検索（AOP）

論理削除・復元・完全削除（管理者）

アーキテクチャ
レイヤ構成

Controller: 入出力制御、バリデーション、画面/API制御

Service: 業務ロジック、トランザクション管理

JPA Repository: エンティティ永続化

MyBatis Mapper: 検索系SQL、集計、監査ログ取得

AOP: 監査ログ・ログ出力・性能計測

Scheduler: 期限近接通知

主な実装クラス

SecurityConfig.java

TodoController.java

TodoService.java

AuditAspect.java

TodoReminderScheduler.java

FileStorageService.java

MailService.java

権限設計
一般ユーザー (ROLE_USER)

自分の ToDo を操作可能

所属グループに紐づく ToDo を閲覧可能（条件付き）

管理者 (ROLE_ADMIN)

/admin/** へアクセス可能

ユーザー管理

グループ管理

監査ログ閲覧

削除済み ToDo の復元 / 完全削除

制御方法:

URL制御: SecurityConfig

メソッド制御: @PreAuthorize

グループ境界判定: TodoService

監査ログ方針

@Auditable アノテーションを付与した処理を AuditAspect で横断記録。

記録項目:

action（操作種別）

username（実行ユーザー）

targetType / targetId

beforeValue / afterValue

createdAt

運用時の追跡性を担保する設計としています。

ローカル起動
前提

Java 17

Maven Wrapper（mvnw）

起動方法（PowerShell）
cd todo
.\mvnw.cmd spring-boot:run


起動後:

http://localhost:8080

H2 Console: http://localhost:8080/h2-console

ポート競合時
Get-NetTCPConnection -LocalPort 8080 -State Listen


代替ポート指定:

$env:SERVER_PORT=18080
.\mvnw.cmd spring-boot:run

設定

主な設定ファイル:

todo/src/main/resources/application.properties

添付ファイル
app.upload.dir=uploads


保存先は実行ディレクトリ基準。
cd todo で起動した場合、実体は todo/uploads。

メール通知

環境変数:

GMAIL_USERNAME

GMAIL_PASSWORD

GMAIL_FROM

テスト・品質保証
ローカルテスト実行
cd todo
.\mvnw.cmd test

CI

GitHub Actions による自動ビルド・テスト

Ubuntu (Linux) 環境で実行

push / pull_request 時に自動実行

現在すべて成功

文字コード対策

UTF-8（BOMなし）で統一

.editorconfig による強制

Linux環境でのコンパイル成功を確認済み

デモ

スクリーンショット格納場所:

docs/README_assets/

補足

本プロジェクトは単なるCRUDアプリではなく、
実務利用を想定した設計（権限・監査・削除設計・CI）まで含めた構成としています。
## Docker起動（PostgreSQL）

既存のローカル起動（H2）はそのまま使えます。Dockerでは `docker` profile で PostgreSQL 接続に切り替えます。

関連ファイル:
- `docker-compose.yml`
- `todo/Dockerfile`
- `todo/src/main/resources/application-docker.properties`
- `.env.example`

### 1) 環境変数ファイルの用意
```bash
cp .env.example .env
```

### 2) 起動
```bash
docker compose up --build
```

起動後:
- アプリ: `http://localhost:8080`（または `.env` の `APP_PORT`）
- DB: PostgreSQL (`db` service)
- Spring Profile: `docker`（`SPRING_PROFILES_ACTIVE=docker`）

### 3) ログイン確認（初期ユーザー）
- 一般ユーザー: `user` / `password`
- 管理者: `admin` / `adminpass`

`application-docker.properties` ではスキーマ変更を行わず検証のみ実施し、初期化は `schema-docker.sql` / `data-docker.sql` をPostgreSQLコンテナ側で適用します。

### ポート競合時
- アプリ 8080 が競合する場合: `.env` の `APP_PORT` を `18080` などへ変更
- PostgreSQL 5432 が競合する場合: `.env` の `POSTGRES_PORT` を変更

## Docker設計方針（運用ガード）

### 初期化責務の分離
- Docker（`SPRING_PROFILES_ACTIVE=docker`）では、**DB初期化はPostgreSQLコンテナ側**で実施します。
- `docker-compose.yml` で `schema-docker.sql` / `data-docker.sql` を `/docker-entrypoint-initdb.d/` にマウントし、DB作成時に一度だけ適用します。
- アプリ側（`application-docker.properties`）は `spring.jpa.hibernate.ddl-auto=validate` と `spring.sql.init.mode=never` を使用し、**二重初期化を防止**します。

### なぜ H2 と PostgreSQL を分離するか
- ローカル開発（`application.properties`）は H2 を使い、素早い起動・試行を優先します。
- Docker 実行（`application-docker.properties`）は PostgreSQL を使い、本番に近い検証を優先します。
- これにより、日常開発の速度と実運用前検証の現実性を両立します。

### なぜ `data-docker.sql` を分離するか
- 既存の `data.sql` は H2 方言を含むため、PostgreSQL ではそのまま互換になりません。
- Docker 用に `data-docker.sql` を分離し、`admin` / `user` を含む最小初期データをPostgreSQLで確実に投入します。

### .env 運用
- Compose は `.env` を前提に動作します（資格情報やポートを直書きしない）。
- 初回は以下を実行してください。
```bash
cp .env.example .env
```
- 代表キー: `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `APP_PORT`, `SPRING_PROFILES_ACTIVE`

### 再起動耐性
- `db` / `app` の両サービスに `restart: unless-stopped` を設定しています。
- DBは `healthcheck` を通過後にアプリが起動するため、起動順序の不整合を抑制します。

## Actuatorヘルスチェック

Docker/CIの疎通確認用に Spring Boot Actuator を有効化しています。

- 疎通確認URL: `http://localhost:8080/actuator/health`
- 公開エンドポイント: `health` のみ
- セキュリティ: `/actuator/health` は `permitAll`（その他は既存の認証/認可を維持）

関連ファイル:
- `todo/pom.xml`
- `todo/src/main/resources/application.properties`
- `todo/src/main/resources/application-docker.properties`
- `todo/src/main/java/com/example/todo/SecurityConfig.java`

### Actuator Note
- `/actuator/health` is the connectivity check endpoint for local, Docker, and CI.
- `management.health.mail.enabled=false` is set to avoid false negatives in environments without SMTP credentials.
- Docker Compose adds an `app` healthcheck that waits for `/actuator/health` to return `{"status":"UP"}`.
- Verify with `docker compose ps` and confirm `todo-app` shows `(healthy)`.

## OpenAPI (Swagger UI)
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Access conditions
- Swagger endpoints (`/swagger-ui/**`, `/v3/api-docs/**`) are public.
- Actual Todo REST API endpoints require authentication (`user/password` or `admin/adminpass` in local seed data).
- You can sign in via `http://localhost:8080/login` before testing APIs in browser.
