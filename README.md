# Health Log

簡單的健康紀錄專案，用來管理與追蹤日常健康資料。

## 基本需求

- Docker 與 Docker Compose
- 或本機具備相容的執行環境（依專案服務需求）

## 快速開始

1. 複製環境變數範例檔：
   - 可參考 `.env.docker.dev.example`
2. 建立本機環境變數檔：
   - 建立 `.env` 並填入必要值
3. 啟動服務：
   - `docker compose up`

## 常用指令

- 啟動：`docker compose up`
- 停止：`docker compose down`
- 查看日誌：`docker compose logs -f`

## 環境變數

請先確認 `.env` 已設定完成，再啟動服務。  
若不確定欄位用途，可先對照 `.env.docker.dev.example`。

Java Web 模組另有本機設定檔範例：  
`health-log-java/app-web/src/main/resources/application-local.properties.example`

建議複製為：  
`health-log-java/app-web/src/main/resources/application-local.properties`  
並依本機環境填入設定（例如 `spring.mail.username`、`spring.mail.password`）。

## 專案結構（待補）

後續可在這裡補上主要目錄與模組說明。
