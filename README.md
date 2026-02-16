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