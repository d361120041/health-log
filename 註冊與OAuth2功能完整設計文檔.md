# 📋 註冊與 OAuth2 功能完整設計文檔

**專案名稱：** 每日身體狀況記錄應用程式 (Health Tracker Web App)  
**最後更新時間：** 2024-12-27  
**基礎認證機制：** JWT 雙 Token（Access Token + Refresh Token）

---

## 📑 目錄

1. [項目概述](#項目概述)
2. [設計方案選擇](#設計方案選擇)
3. [實作策略](#實作策略)
4. [資料庫變更說明](#資料庫變更說明)
5. [階段一驗證報告](#階段一驗證報告)
6. [後續實作步驟](#後續實作步驟)
7. [API 端點設計](#api-端點設計)
8. [待確認事項](#待確認事項)

---

## 📋 項目概述

### 目標功能

本專案需要實作兩種認證方式：

1. **Email 驗證註冊**（方案二）
   - 使用者使用 Email 和密碼註冊
   - 註冊後發送驗證郵件
   - 點擊驗證連結後才能登入
   - 預設角色為 `USER`，但 `is_active = false`（未驗證）

2. **OAuth2 Google 登入/註冊**
   - 使用者可以使用 Google 帳號登入
   - 首次使用時自動註冊
   - 預設角色為 `USER`，`is_active = true`（Google 已驗證 Email）

### 核心架構

兩種認證方式最終都統一生成相同的 JWT Token 格式，前端使用相同的認證機制。

```
┌─────────────────────────────────────────────────────────┐
│                     認證方式                            │
├──────────────────┬──────────────────────────────────────┤
│  Email + 密碼    │     Google OAuth2                    │
│  註冊/登入       │     登入/註冊                        │
└────────┬─────────┴──────────────┬───────────────────────┘
         │                        │
         ▼                        ▼
    ┌──────────────────────────────────────┐
    │     AuthService（統一認證服務）       │
    │  - register()                        │
    │  - login()                           │
    │  - oauth2Login()                     │
    └──────────────┬───────────────────────┘
                   │
                   ▼
    ┌──────────────────────────────────────┐
    │     生成 JWT Token（統一格式）        │
    │  - Access Token                      │
    │  - Refresh Token（存於 Redis）        │
    └──────────────┬───────────────────────┘
                   │
                   ▼
        前端 authStore（統一處理）
```

---

## 🎯 設計方案選擇

### 註冊功能方案比較

| 特性 | 方案一：簡單公開 | 方案二：Email 驗證 | 方案三：管理員審核 | 方案四：邀請碼 |
|------|----------------|------------------|------------------|--------------|
| **實作複雜度** | ⭐ 低 | ⭐⭐⭐ 中 | ⭐⭐ 中低 | ⭐⭐⭐ 中高 |
| **安全性** | ⭐ 低 | ⭐⭐⭐ 高 | ⭐⭐⭐⭐ 很高 | ⭐⭐⭐ 高 |
| **使用者體驗** | ⭐⭐⭐⭐ 很好 | ⭐⭐⭐ 好 | ⭐⭐ 普通 | ⭐⭐⭐ 好 |
| **防止垃圾帳號** | ❌ 無 | ✅ 有效 | ✅ 非常有效 | ✅ 有效 |
| **額外依賴** | 無 | Spring Mail | 無 | 無 |
| **擴展性** | ⭐⭐ 低 | ⭐⭐⭐⭐ 高 | ⭐⭐ 低 | ⭐⭐⭐ 中 |

### 選擇結果

**✅ 推薦使用「方案二：Email 驗證註冊」**

理由：
1. ✅ **安全性平衡**：既保證 Email 有效性，又不會過於複雜
2. ✅ **使用者體驗**：雖然需要驗證步驟，但這是業界標準做法
3. ✅ **未來擴展**：可以基於 Email 驗證實現密碼重設功能
4. ✅ **實作難度**：適中，Spring Boot 對郵件服務支援完善
5. ✅ **成本**：可以使用免費的 SMTP 服務（Gmail、SendGrid 等）

### OAuth2 方案比較

| 特性 | 方案 A：後端重定向 | 方案 B：前端 SDK | 方案 C：混合模式 |
|------|------------------|-----------------|-----------------|
| **實作複雜度** | ⭐⭐⭐ 中 | ⭐⭐ 低 | ⭐⭐⭐⭐ 高 |
| **安全性** | ⭐⭐⭐⭐⭐ 很高 | ⭐⭐⭐⭐ 高 | ⭐⭐⭐⭐ 高 |
| **SPA 體驗** | ⭐⭐⭐ 好 | ⭐⭐⭐⭐⭐ 很好 | ⭐⭐⭐⭐ 很好 |
| **標準符合度** | ⭐⭐⭐⭐⭐ 完全符合 | ⭐⭐⭐ 部分符合 | ⭐⭐⭐ 部分符合 |
| **推薦程度** | ⭐⭐⭐⭐ 推薦 | ⭐⭐⭐⭐⭐ 最推薦 | ⭐⭐ 不推薦 |

### 選擇結果

**✅ 推薦使用「方案 B：前端 Google Sign-In + 後端驗證」**

理由：
1. ✅ **實作簡單**：不需要處理複雜的重定向邏輯
2. ✅ **SPA 體驗好**：不會打斷使用者流程
3. ✅ **安全性足夠**：後端驗證 ID Token，確保真實性
4. ✅ **與現有架構整合容易**：最終統一生成 JWT Token
5. ✅ **維護成本低**：邏輯清晰，容易理解和維護

---

## 🎯 實作策略

### 推薦策略：混合策略 - 一起規劃，分階段實作

#### 為什麼不建議完全照順序？

❌ **完全照順序的缺點：**
- 需要修改 User Entity 兩次（先改 Email 驗證，再改 OAuth2）
- 可能導致資料庫 Migration 重複
- 如果先完成 Email 驗證，之後加入 OAuth2 可能還需要調整 Email 驗證的邏輯

❌ **完全一起實作的缺點：**
- 如果兩個功能都出問題，難以定位
- 測試複雜度較高
- 程式碼審查時變更範圍太大

### 三階段混合實作

#### 📌 階段一：資料基礎層（一起實作）⭐ 必須一起

**目標：** 建立支援兩種認證方式的資料基礎

**需要修改：**
1. ✅ **User Entity** - 一次性修改到位
   - 新增 `oauth2Provider` 欄位
   - 新增 `oauth2Id` 欄位  
   - `passwordHash` 改為 `nullable = true`
   - 新增 Email 驗證相關欄位（`emailVerificationToken`, `emailVerifiedAt` 等）

2. ✅ **資料庫 Migration** - 一次完成所有欄位變更

3. ✅ **UserRepository** - 擴充查詢方法
   - `findByEmailVerificationToken()`
   - `findByOAuth2ProviderAndOAuth2Id()`

**為什麼一起實作？**
- 避免重複修改 Entity
- 資料庫 Migration 只執行一次
- 基礎架構一次到位，後續開發更順暢

**預計時間：** 45-90 分鐘

---

#### 📌 階段二：Email 驗證註冊（先實作）⭐ 相對簡單

**目標：** 完成 Email 註冊和驗證流程

**需要實作：**
1. ✅ **EmailService** - 郵件發送服務
   - 發送驗證郵件
   - 郵件模板

2. ✅ **AuthService 擴充**
   - `register()` - 註冊並發送驗證郵件
   - `verifyEmail()` - 驗證 Email Token

3. ✅ **AuthController 新增端點**
   - `POST /api/auth/register`
   - `POST /api/auth/verify-email`
   - `POST /api/auth/resend-verification`（可選）

4. ✅ **前端實作**
   - `Register.vue` - 註冊頁面
   - `VerifyEmail.vue` - 驗證頁面
   - `authStore.register()` 方法

**為什麼先實作？**
- 邏輯相對獨立，不需要額外的 OAuth2 配置
- 可以驗證資料庫變更是否正確
- 測試較簡單，可以先確認基礎功能正常

**測試重點：**
- ✅ 註冊流程
- ✅ 郵件發送
- ✅ Email 驗證
- ✅ 驗證後登入

**預計時間：** 3-5 小時

---

#### 📌 階段三：OAuth2 Google 登入（後實作）⭐ 配置較複雜

**目標：** 完成 OAuth2 Google 登入/註冊

**需要實作：**
1. ✅ **依賴新增**
   - `com.google.auth:google-auth-library-oauth2-http`

2. ✅ **AuthController 新增端點**
   - `POST /api/auth/oauth2/google`

3. ✅ **AuthService 擴充**
   - `loginWithGoogle(String idToken)` - OAuth2 使用者查找/創建邏輯

4. ✅ **新增工具類**
   - `GoogleTokenVerifier` - 驗證 Google ID Token

5. ✅ **前端實作**
   - `Login.vue` - 新增「使用 Google 登入」按鈕
   - 使用 Google Identity Services JavaScript SDK
   - `authStore.loginWithGoogle()` 方法

**為什麼後實作？**
- 需要 Google OAuth2 憑證配置
- 在 Email 註冊基礎上實作，邏輯更清晰

**測試重點：**
- ✅ Google OAuth2 認證
- ✅ OAuth2 回調處理
- ✅ 新使用者自動註冊
- ✅ 已存在使用者登入
- ✅ JWT Token 生成

**預計時間：** 3-4.5 小時

---

### 實作時程規劃

```
階段一：資料基礎層（一起實作）
├─ User Entity 修改              [30-60 分鐘]
└─ UserRepository 擴充            [15-30 分鐘]
──────────────────────────────────────────
總計：45-90 分鐘

階段二：Email 驗證註冊（先實作）
├─ EmailService                  [60-90 分鐘]
├─ AuthService 擴充              [60-90 分鐘]
├─ AuthController 端點           [30-60 分鐘]
├─ 前端 Register/Verify          [60-90 分鐘]
└─ 測試                          [30-60 分鐘]
──────────────────────────────────────────
總計：3-5 小時

階段三：OAuth2 Google（後實作）
├─ Google OAuth2 配置            [30-60 分鐘]
├─ GoogleTokenVerifier           [30-60 分鐘]
├─ AuthService 擴充              [30-60 分鐘]
├─ 前端 OAuth2 整合              [30-60 分鐘]
└─ 測試                          [30-60 分鐘]
──────────────────────────────────────────
總計：3-4.5 小時

總計時間：6.5-10 小時（約 1-1.5 個工作天）
```

---

## 🗄️ 資料庫變更說明

### `users` 表結構變更

#### 1. 修改現有欄位

| 欄位名稱 | 變更前 | 變更後 | 說明 |
|---------|--------|--------|------|
| `password_hash` | `NOT NULL` | `NULL` | OAuth2 使用者不需要密碼，改為可選 |

#### 2. 新增欄位

| 欄位名稱 | 資料類型 | 約束 | 說明 |
|---------|---------|------|------|
| `oauth2_provider` | `VARCHAR(50)` | `NULL` | OAuth2 提供者（如 'GOOGLE'） |
| `oauth2_id` | `VARCHAR(255)` | `NULL` | OAuth2 使用者 ID |
| `email_verification_token` | `VARCHAR(255)` | `NULL` | Email 驗證 Token |
| `email_verified_at` | `TIMESTAMP WITH TIME ZONE` | `NULL` | Email 驗證時間 |

#### 3. 新增索引

| 索引名稱 | 欄位 | 類型 | 說明 |
|---------|------|------|------|
| `idx_users_oauth2` | `oauth2_provider, oauth2_id` | INDEX | 加速 OAuth2 使用者查詢 |
| `idx_users_email_verification_token` | `email_verification_token` | INDEX | 加速 Email 驗證 Token 查詢 |

### SQL Migration 語句（PostgreSQL）

```sql
-- 1. 修改 password_hash 欄位為可選
ALTER TABLE users 
  ALTER COLUMN password_hash DROP NOT NULL;

-- 2. 新增 OAuth2 相關欄位
ALTER TABLE users 
  ADD COLUMN oauth2_provider VARCHAR(50) NULL,
  ADD COLUMN oauth2_id VARCHAR(255) NULL;

-- 3. 新增 Email 驗證相關欄位
ALTER TABLE users 
  ADD COLUMN email_verification_token VARCHAR(255) NULL,
  ADD COLUMN email_verified_at TIMESTAMP WITH TIME ZONE NULL;

-- 4. 新增索引
CREATE INDEX idx_users_oauth2 ON users(oauth2_provider, oauth2_id);
CREATE INDEX idx_users_email_verification_token ON users(email_verification_token);

-- 5. 可選：添加唯一約束（確保同一 OAuth2 提供者的 ID 唯一）
CREATE UNIQUE INDEX idx_users_oauth2_unique 
ON users(oauth2_provider, oauth2_id) 
WHERE oauth2_provider IS NOT NULL AND oauth2_id IS NOT NULL;
```

### 資料邏輯說明

#### Email 註冊使用者
- `email`: ✅ 必填
- `password_hash`: ✅ 必填（bcrypt 加密密碼）
- `oauth2_provider`: ❌ NULL
- `oauth2_id`: ❌ NULL
- `email_verification_token`: ✅ 註冊時生成，驗證後清除
- `email_verified_at`: ❌ 註冊時為 NULL，驗證後設置時間
- `is_active`: ❌ `false`（待驗證）→ ✅ `true`（驗證後）

#### OAuth2 Google 使用者
- `email`: ✅ 必填（Google Email）
- `password_hash`: ❌ NULL（不需要密碼）
- `oauth2_provider`: ✅ 'GOOGLE'
- `oauth2_id`: ✅ Google 使用者 ID（sub）
- `email_verification_token`: ❌ NULL
- `email_verified_at`: ✅ 註冊時設置（Google 已驗證）
- `is_active`: ✅ `true`（Google 已驗證 Email）

### 注意事項

1. **現有資料處理**
   - 現有的使用者資料不需要變更
   - 所有現有使用者的 `password_hash` 仍然存在
   - 新欄位預設為 NULL，不影響現有功能

2. **資料完整性**
   - `password_hash` 和 OAuth2 欄位至少有一個不為 NULL（應用層檢查）
   - Email 仍然保持唯一性

3. **索引效能**
   - OAuth2 索引用於加速 `findByOAuth2ProviderAndOAuth2Id` 查詢
   - Email 驗證 Token 索引用於加速驗證流程

---

## ✅ 階段一驗證報告

**驗證時間：** 2024-12-27  
**狀態：** ✅ 通過

### 驗證項目

#### 1. ✅ Entity 編譯檢查

**User Entity 修改**
- ✅ 新增 `oauth2Provider` 欄位（可選）
- ✅ 新增 `oauth2Id` 欄位（可選）
- ✅ 新增 `emailVerificationToken` 欄位（可選）
- ✅ 新增 `emailVerifiedAt` 欄位（可選）
- ✅ `passwordHash` 改為 `nullable = true`
- ✅ 新增索引：`idx_users_oauth2`、`idx_users_email_verification_token`

**編譯結果：** ✅ 成功

#### 2. ✅ Repository 擴充檢查

**UserRepository 新增方法**
- ✅ `findByEmailVerificationToken(String token)` - 用於 Email 驗證
- ✅ `findByOAuth2ProviderAndOAuth2Id(String provider, String oauth2Id)` - 用於 OAuth2 登入

**編譯結果：** ✅ 成功

#### 3. ✅ Service 層調整檢查

**AuthService 修改**
- ✅ `login()` 方法：新增檢查 `passwordHash` 為 null 的情況（拒絕 OAuth2 使用者使用密碼登入）
- ✅ `loadUserByUsername()` 方法：處理 `passwordHash` 為 null 的情況（設置 `{noop}` 作為預設值）

**修改說明：**
```java
// login() 方法中
if (user.getPasswordHash() == null || user.getPasswordHash().isEmpty()) {
    throw new BadCredentialsException("Invalid email or password");
}

// loadUserByUsername() 方法中
String password = user.getPasswordHash() != null 
    ? user.getPasswordHash() 
    : "{noop}"; // OAuth2 使用者不會通過這個方法登入
```

**編譯結果：** ✅ 成功

#### 4. ✅ 資料庫結構驗證

**資料庫變更狀態：** ✅ 已完成（用戶已手動執行）

**變更內容確認：**
- ✅ `password_hash` 欄位已改為可選（NULL）
- ✅ 新增 `oauth2_provider` 欄位
- ✅ 新增 `oauth2_id` 欄位
- ✅ 新增 `email_verification_token` 欄位
- ✅ 新增 `email_verified_at` 欄位
- ✅ 新增索引 `idx_users_oauth2`
- ✅ 新增索引 `idx_users_email_verification_token`

### 程式碼檢查

#### User Entity 結構
```java
@Column(name = "password_hash", nullable = true)  // ✅ 可選
private String passwordHash;

@Column(name = "oauth2_provider", nullable = true)
private String oauth2Provider;

@Column(name = "oauth2_id", nullable = true)
private String oauth2Id;

@Column(name = "email_verification_token", nullable = true)
private String emailVerificationToken;

@Column(name = "email_verified_at", nullable = true)
private OffsetDateTime emailVerifiedAt;
```

#### UserRepository 方法
```java
Optional<User> findByEmailVerificationToken(String token);  // ✅
Optional<User> findByOAuth2ProviderAndOAuth2Id(String oauth2Provider, String oauth2Id);  // ✅
```

### 注意事項

#### 1. 現有功能相容性

- ✅ **現有登入功能**：正常運作（仍使用密碼登入）
- ✅ **現有使用者資料**：不受影響（所有現有使用者都有 `passwordHash`）
- ✅ **測試檔案**：不需要修改（測試中使用有密碼的使用者）

#### 2. 新功能準備

- ✅ Entity 結構已支援 Email 驗證註冊
- ✅ Entity 結構已支援 OAuth2 Google 登入
- ✅ Repository 方法已準備好供後續使用

#### 3. 資料完整性

- ✅ Email 仍然保持唯一性
- ✅ `passwordHash` 和 OAuth2 欄位至少有一個不為 NULL（應用層邏輯確保）
- ✅ 現有資料完整性不受影響

### 驗證結論

**階段一（資料基礎層）已完成並驗證通過：**

1. ✅ Entity 編譯成功
2. ✅ Repository 擴充成功
3. ✅ Service 層調整成功
4. ✅ 資料庫結構已更新
5. ✅ 現有功能不受影響
6. ✅ 程式可以正常編譯

**建議：** 可以繼續進行階段二的實作（Email 驗證註冊）。

---

## 📝 後續實作步驟

### 階段二：Email 驗證註冊

**準備工作已完成：**
- ✅ User Entity 已包含 Email 驗證欄位
- ✅ UserRepository 已包含查詢方法

**接下來需要實作：**
1. EmailService（郵件發送服務）
2. AuthService 擴充（register, verifyEmail 方法）
3. AuthController 新增端點
4. 前端實作（Register.vue, VerifyEmail.vue）

### 階段三：OAuth2 Google 登入

**準備工作已完成：**
- ✅ User Entity 已包含 OAuth2 欄位
- ✅ UserRepository 已包含查詢方法

**接下來需要實作：**
1. Google OAuth2 依賴和配置
2. GoogleTokenVerifier（驗證 Google ID Token）
3. AuthService 擴充（loginWithGoogle 方法）
4. AuthController 新增端點
5. 前端 OAuth2 整合（Google 登入按鈕、回調處理）

---

## 🔧 API 端點設計

### 新增端點

```
POST /api/auth/register
  Request: { email, password, confirmPassword }
  Response: { message: "驗證郵件已發送" }

POST /api/auth/verify-email
  Request: { token }
  Response: { message: "驗證成功" }

POST /api/auth/oauth2/google
  Request: { idToken }
  Response: AuthResponseDTO {
    accessToken,
    tokenType: "Bearer",
    expiresIn
  }
```

### 現有端點保持不變

```
POST /api/auth/login        // Email + 密碼登入
POST /api/auth/refresh      // 刷新 Token
POST /api/auth/logout       // 登出
```

---

## 🔑 Google OAuth2 設定步驟

1. **前往 Google Cloud Console**
   - https://console.cloud.google.com/

2. **創建專案**（如果還沒有）

3. **啟用 Google+ API**
   - APIs & Services > Library > 搜索 "Google+ API" > Enable

4. **創建 OAuth 2.0 憑證**
   - APIs & Services > Credentials > Create Credentials > OAuth client ID
   - Application type: Web application
   - Authorized JavaScript origins: `http://localhost:5173`（開發環境）
   - Authorized redirect URIs: `http://localhost:8080/api/auth/oauth2/callback/google`（如果使用方案 A）

5. **獲取 Client ID 和 Client Secret**
   - 將 Client ID 配置到前端環境變數（`VITE_GOOGLE_CLIENT_ID`）
   - 將 Client Secret 配置到後端 `application.properties`（如果使用方案 A）

---

## ⚠️ 關鍵注意事項

### 1. User Entity 修改策略

**建議一次修改到位：**
```java
// 一次加入所有需要的欄位
@Column(name = "password_hash", nullable = true)  // 改為可選
private String passwordHash;

@Column(name = "oauth2_provider")
private String oauth2Provider;

@Column(name = "oauth2_id")
private String oauth2Id;

@Column(name = "email_verification_token")
private String emailVerificationToken;

@Column(name = "email_verified_at")
private OffsetDateTime emailVerifiedAt;
```

**為什麼？**
- 避免多次 Migration
- 避免程式碼重構
- 資料結構一次到位

### 2. 測試策略

**階段一完成後：**
- ✅ 測試 User Entity 的保存和查詢
- ✅ 確認資料庫欄位正確

**階段二完成後：**
- ✅ 測試 Email 註冊完整流程
- ✅ 確認 Email 驗證機制正常
- ✅ 測試現有登入功能是否受影響

**階段三完成後：**
- ✅ 測試 OAuth2 登入流程
- ✅ 測試 Email 和 OAuth2 兩種登入方式互不干擾
- ✅ 測試邊界情況（同 Email 既有密碼又有 OAuth2）

### 3. 邊界情況處理

**需要考慮的情況：**
- ✅ 同一個 Email，先用 Email 註冊，後用 OAuth2 登入
- ✅ 同一個 Email，先用 OAuth2 登入，後用 Email 註冊
- ✅ 使用者已有 Email 帳號，嘗試用 OAuth2 綁定

**建議處理方式：**
- 如果 Email 已存在且沒有 OAuth2 綁定，返回錯誤，提示使用原方式登入（更安全）
- 或者：詢問使用者是否要綁定（需要額外的 UI 和邏輯）

### 4. 安全性

- ✅ Google ID Token 必須在後端驗證
- ✅ 不能信任前端傳來的未驗證 Token
- ✅ 使用 HTTPS（生產環境）
- ✅ Email 驗證 Token 應該有過期時間（建議 24 小時）

### 5. 密碼強度

- ✅ 建議實作密碼強度檢查（前端 + 後端）
- ✅ 最小長度、包含大小寫字母和數字等

### 6. 錯誤處理

- ✅ Email 已存在時返回友好錯誤訊息
- ✅ 不要洩露系統內部資訊
- ✅ 資料庫事務：註冊過程應該在事務中執行，確保資料一致性

---

## 📝 待確認事項

在開始實作前，請確認：

- [x] 選擇註冊方案：**方案二（Email 驗證註冊）**
- [x] 選擇 OAuth2 方案：**方案 B（前端 Google Sign-In + 後端驗證）**
- [ ] 是否需要密碼強度要求？
- [ ] SMTP 服務配置（Gmail、SendGrid 等）？
- [ ] 註冊後是否自動登入，還是需要手動登入？
- [ ] 是否需要「忘記密碼」功能（可以一併實作）？
- [ ] OAuth2 使用者是否需要 Email 驗證？（建議不需要，因為 Google 已驗證）
- [ ] 是否允許「Email 使用者綁定 OAuth2」或「OAuth2 使用者設定密碼」？
- [ ] Google OAuth2 Client ID 和 Client Secret 的獲取方式？
- [ ] 生產環境的 OAuth2 重定向 URI 配置？

---

## 🔗 相關資源

- [Google Identity Services 文檔](https://developers.google.com/identity/gsi/web)
- [Spring Security OAuth2 Client](https://docs.spring.io/spring-security/reference/servlet/oauth2/client/index.html)
- [Google Auth Library for Java](https://github.com/googleapis/google-auth-library-java)
- [Spring Mail 文檔](https://docs.spring.io/spring-framework/reference/integration/email.html)

---

**文檔版本：** 1.0  
**最後更新：** 2024-12-27  
**狀態：** 階段一已完成 ✅，階段二和階段三待實作

