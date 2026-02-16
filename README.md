# ToDo App (Spring Boot)
[![CI](https://github.com/202510-aw-sugihara/ToDo2602/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/202510-aw-sugihara/ToDo2602/actions/workflows/ci.yml)

## 概要
このリポジトリは、Spring Boot 3.5 / Thymeleaf を使った ToDo 管理アプリです。  
基本的な ToDo 管理に加え、認証/認可、グループ境界、添付ファイル、CSV 出力、メール通知、監査ログ、論理削除を実装しています。

関連クラス/設定:
- `todo/src/main/java/com/example/todo/TodoApplication.java`
- `todo/src/main/resources/application.properties`

## 対象ユーザー
- 個人または小規模チームで、期限・状態・カテゴリを使ってタスク管理したい人
- 管理者としてユーザー/グループ管理、監査ログ確認、削除済み ToDo の復元や完全削除を行いたい人

関連クラス:
- `todo/src/main/java/com/example/todo/TodoController.java`
- `todo/src/main/java/com/example/todo/AdminUserController.java`
- `todo/src/main/java/com/example/todo/AdminGroupController.java`
- `todo/src/main/java/com/example/todo/AdminDeletedTodoController.java`

## 解決する課題
- ToDo の作成・更新・検索・状態管理を一元化する
- ユーザー単位 + グループ単位で閲覧境界を制御する
- 変更履歴を監査ログとして残し、運用時の追跡可能性を確保する
- 誤削除時に復元可能な論理削除を提供し、管理者が完全削除も実施できる

関連クラス:
- `todo/src/main/java/com/example/todo/TodoService.java`
- `todo/src/main/java/com/example/todo/AuditAspect.java`
- `todo/src/main/java/com/example/todo/AuditLogService.java`

## 主要機能
- 認証/認可（ログイン、登録、ロール制御）
- ToDo CRUD（Web + REST API）
- カテゴリ管理
- グループ管理（会社/部署/案件/顧客などの階層）
- 添付ファイルのアップロード・ダウンロード・プレビュー
- CSV エクスポート
- 期限近接 ToDo のメール通知
- 監査ログの記録/検索
- 論理削除・復元・完全削除（管理者）

主要ファイル:
- `todo/src/main/java/com/example/todo/SecurityConfig.java`
- `todo/src/main/java/com/example/todo/TodoController.java`
- `todo/src/main/java/com/example/todo/TodoApiController.java`
- `todo/src/main/java/com/example/todo/CategoryController.java`
- `todo/src/main/java/com/example/todo/AdminGroupController.java`
- `todo/src/main/java/com/example/todo/TodoAttachmentService.java`
- `todo/src/main/java/com/example/todo/MailService.java`
- `todo/src/main/java/com/example/todo/TodoReminderScheduler.java`
- `todo/src/main/java/com/example/todo/AuditLogAdminController.java`

## アーキテクチャ
- Controller: 画面/API 入出力、バリデーション、遷移
- Service: 業務ロジック、トランザクション、監査呼び出し
- JPA Repository: エンティティ永続化
- MyBatis Mapper: 検索/集計/更新 SQL（一覧検索、件数、監査検索、添付）
- AOP: 監査・ログ・性能計測
- Scheduler: 期限近接 ToDo の定期通知

主要ファイル:
- Controller: `todo/src/main/java/com/example/todo/*Controller.java`
- Service: `todo/src/main/java/com/example/todo/*Service.java`
- JPA: `todo/src/main/java/com/example/todo/*Repository.java`
- MyBatis: `todo/src/main/java/com/example/todo/*Mapper.java`, `todo/src/main/resources/mapper/*.xml`
- AOP: `todo/src/main/java/com/example/todo/AuditAspect.java`, `todo/src/main/java/com/example/todo/LoggingAspect.java`, `todo/src/main/java/com/example/todo/PerformanceAspect.java`
- Scheduler: `todo/src/main/java/com/example/todo/TodoReminderScheduler.java`

## 権限設計
- 一般ユーザー (`ROLE_USER`):
  - 自分の ToDo を操作
  - 自分が所属するグループに紐づく ToDo を閲覧可能（条件付き）
- 管理者 (`ROLE_ADMIN`):
  - `/admin/**` を利用可能
  - ユーザー管理、グループ管理、監査ログ閲覧、削除済み ToDo の復元/完全削除

設計の要点:
- 画面/API の入口制御: `SecurityConfig.java`
- 管理画面の追加制約: `@PreAuthorize`（`AdminUserController.java`, `AdminGroupController.java`, `AdminDeletedTodoController.java`, `AuditLogAdminController.java`）
- グループ境界を使った閲覧判定: `TodoService.java` / `TodoController.java`
- 論理削除は通常操作、復元/完全削除は管理責務: `TodoService.java`, `AdminDeletedTodoController.java`

## 監査ログ方針
- `@Auditable` の付いた処理を `AuditAspect` で横断記録
- 主要な記録項目:
  - action（操作種別）
  - username（実行ユーザー）
  - targetType / targetId（対象）
  - beforeValue / afterValue（前後値）
  - createdAt（記録時刻）
- 手動記録も併用（例: バルク削除、作成/更新/削除）

関連ファイル:
- `todo/src/main/java/com/example/todo/Auditable.java`
- `todo/src/main/java/com/example/todo/AuditAspect.java`
- `todo/src/main/java/com/example/todo/AuditLogService.java`
- `todo/src/main/java/com/example/todo/AuditLogMapper.java`
- `todo/src/main/resources/mapper/AuditLogMapper.xml`
- `todo/src/main/resources/schema.sql`

## ローカル起動
### 前提
- Java 17
- Maven Wrapper（`mvnw` / `mvnw.cmd`）

### 起動手順（PowerShell）
```powershell
cd todo
.\mvnw.cmd spring-boot:run
```

起動後:
- アプリ: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`

### 8080 ポート競合時の対処
`Port 8080 was already in use` が出る場合があります。

1) 利用中プロセスの特定（Windows）
```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen
```
`OwningProcess` の PID を確認し、必要に応じて停止してください。

2) 代替ポートを使う方法
- `application.properties` を変更
  - `todo/src/main/resources/application.properties` に `server.port=18080` を追加
- 環境変数で上書き
```powershell
$env:SERVER_PORT=18080
cd todo
.\mvnw.cmd spring-boot:run
```
- 起動引数で上書き
```powershell
cd todo
.\mvnw.cmd "-Dspring-boot.run.arguments=--server.port=18080" spring-boot:run
```

## 設定
主要設定ファイル:
- `todo/src/main/resources/application.properties`

ポイント:
- DB（H2, In-Memory）
  - `spring.datasource.url=jdbc:h2:mem:tododb...`
- メール
  - `spring.mail.*`
  - `app.mail.from`
  - 環境変数: `GMAIL_USERNAME`, `GMAIL_PASSWORD`, `GMAIL_FROM`
- 添付ファイル
  - `app.upload.dir=uploads`
  - **保存先は実行ディレクトリ基準**です。
  - `cd todo` で起動した場合、実体は `todo/uploads` になります。
- アップロード上限
  - `spring.servlet.multipart.max-file-size=10MB`
  - `spring.servlet.multipart.max-request-size=10MB`

関連クラス:
- `todo/src/main/java/com/example/todo/FileStorageService.java`
- `todo/src/main/java/com/example/todo/MailService.java`

## テスト（現況・前提・整備方針）
実行手順:
```powershell
cd todo
.\mvnw.cmd test
```

現況（2026-02-16 時点）:
- 現状のままだと一部テストは失敗します。
- 主な既知課題:
  - `todo/src/test/java/com/example/todo/TodoServiceMapperMockTest.java`
    - `TodoService` / `TodoMapper` の現行シグネチャとの不整合
  - `todo/src/test/java/com/example/todo/TodoMapperTest.java`
    - Mapper メソッド呼び出しシグネチャが古い前提
  - `todo/src/test/resources/schema-mybatis.sql`
    - `completed` 前提のスキーマで、本体 `Todo` の `status` ベース設計と不整合

今後の整備方針:
- テストコードを現行の `status` / `deletedAt` / Mapper 引数に合わせて更新
- `schema-mybatis.sql` / `data-mybatis.sql` を本体スキーマへ追従
- Web/Service/Mapper の回帰を段階的に再構築

## 文字コード運用ルール（UTF-8 統一）
文字化け回避のため、以下を UTF-8 で統一します。
- `todo/src/main/resources/data.sql`
- `todo/src/main/resources/messages_ja_JP.properties`
- `README.md`

運用ルール:
- IDE のファイルエンコーディングを UTF-8 に固定
- Git へ保存する前に、対象ファイルが UTF-8 であることを確認
- SQL/プロパティ/README の日本語編集時は、エディタの自動変換設定に注意
- 必要に応じて Maven 実行時に UTF-8 を明示（例: `JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8`）

## デモ
スクリーンショット置き場（画像は未追加）:
- `docs/README_assets/`
