-- ============================================
-- Health Log 資料庫初始化腳本
-- PostgreSQL
-- ============================================

-- 1. 身份與權限管理
-- ============================================

-- A. 角色表
CREATE TABLE IF NOT EXISTS roles (
    role_id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- B. 使用者表
CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id INT NOT NULL REFERENCES roles(role_id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 建立 users 表的索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- ============================================
-- 2. 記錄配置管理 (EAV Attribute)
-- ============================================

-- C. 欄位設定表
CREATE TABLE IF NOT EXISTS field_settings (
    setting_id SERIAL PRIMARY KEY,
    field_name VARCHAR(100) NOT NULL UNIQUE,
    data_type VARCHAR(20) NOT NULL,
    unit VARCHAR(50),
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    options TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================
-- 3. 每日記錄與數據 (EAV Entity & Value)
-- ============================================

-- D. 每日記錄主表
CREATE TABLE IF NOT EXISTS daily_records (
    record_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    record_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE(user_id, record_date)
);

-- E. 記錄數值表 (EAV Value)
CREATE TABLE IF NOT EXISTS record_data (
    data_id BIGSERIAL PRIMARY KEY,
    record_id BIGINT NOT NULL REFERENCES daily_records(record_id) ON DELETE CASCADE,
    setting_id INT NOT NULL REFERENCES field_settings(setting_id),
    value_text TEXT NOT NULL,
    UNIQUE(record_id, setting_id)
);

-- ============================================
-- 4. 效能優化索引
-- ============================================

-- daily_records 複合唯一索引（已在表定義中建立）
-- record_data 複合唯一索引（已在表定義中建立）

-- 趨勢查詢優化索引：加速按特定欄位進行篩選和時間排序
CREATE INDEX IF NOT EXISTS idx_record_data_setting_record 
ON record_data(setting_id, record_id);

-- ============================================
-- 5. 初始資料
-- ============================================

-- 插入預設角色
INSERT INTO roles (role_name) VALUES ('ADMIN'), ('USER')
ON CONFLICT (role_name) DO NOTHING;

