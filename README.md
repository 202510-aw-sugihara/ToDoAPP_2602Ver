# Todo (Spring Boot)

Spring Boot 3.5 + Thymeleaf で構成した ToDo 管理アプリです。ユーザー認証、カテゴリ・グループ管理、添付ファイル、CSV エクスポート、期限通知メール、監査ログなどを含みます。

## 主要機能
1. 認証/認可（フォームログイン、管理者画面）
2. ToDo CRUD（一覧・詳細・作成・編集・削除）
3. 添付ファイルのアップロード/プレビュー/ダウンロード
4. カテゴリ/グループでの分類とアクセス制御
5. CSV エクスポート（検索条件を反映）
6. 期限が近い ToDo のメール通知
7. 監査ログ（AOP で操作履歴を記録）
8. 削除済み ToDo の復元/完全削除（管理者）

## 前提
1. Java 17
2. Maven（`mvnw`/`mvnw.cmd` あり）

## 起動方法
PowerShell の例です。

```powershell
cd todo
.\mvnw.cmd spring-boot:run
```

起動後は `http://localhost:8080` にアクセスします。H2 コンソールは `http://localhost:8080/h2-console` です。

## 初期ユーザー
`src/main/resources/data.sql` に初期データがあります。

1. 一般ユーザー: `user` / `password`
2. 管理者: `admin` / `adminpass`

## 管理者機能
管理者ユーザーでログインすると以下にアクセスできます。

1. `GET /admin/users` ユーザー管理
2. `GET /admin/groups` グループ管理
3. `GET /admin/audit-logs` 監査ログ
4. `GET /admin/deleted-todos` 削除済み ToDo の復元/完全削除

## 設定メモ
`todo/src/main/resources/application.properties` の主な設定です。

1. H2: `jdbc:h2:mem:tododb`
2. アップロード先: `app.upload.dir=uploads`
3. メール: `GMAIL_USERNAME` / `GMAIL_PASSWORD` / `GMAIL_FROM`
4. 添付ファイル上限: 10MB

## テスト
```powershell
cd todo
.\mvnw.cmd test
```
