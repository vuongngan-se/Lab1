-- ==============================================================================
-- KỊCH BẢN TẠO CƠ SỞ DỮ LIỆU MYSQL CHO LAB 5 RMI ĐỒNG BỘ DỮ LIỆU (VKU - LTM)
-- ==============================================================================
-- Mô hình kiến trúc phân tán Active-Active:
-- - Server 1 lưu trữ dữ liệu tại: db_server1
-- - Server 2 lưu trữ dữ liệu tại: db_server2
-- (Lưu ý: Hệ thống Java đã tích hợp tính năng tự động tạo Database & Table nếu chưa có,
--  bạn chỉ cần bật MySQL trên XAMPP là hệ thống tự khởi tạo!)
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. CƠ SỞ DỮ LIỆU CHO SERVER 1
-- ------------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS db_server1 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_server1;

CREATE TABLE IF NOT EXISTS records (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    score DOUBLE NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    last_modified BIGINT NOT NULL,
    updated_by_server VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_events (
    id VARCHAR(50) PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    record_id VARCHAR(50) NOT NULL,
    origin_server VARCHAR(50) NOT NULL,
    target_server VARCHAR(50) NOT NULL,
    description TEXT
);

-- Dữ liệu mẫu ban đầu cho Server 1
INSERT INTO records (id, name, department, score, version, last_modified, updated_by_server) VALUES
('24NS001', 'Nguyễn Văn An', 'Khoa Học Máy Tính', 3.85, 1, UNIX_TIMESTAMP() * 1000, 'SERVER_1'),
('24NS002', 'Trần Thị Bình', 'Kỹ Thuật Phần Mềm', 3.70, 1, UNIX_TIMESTAMP() * 1000, 'SERVER_1'),
('24NS003', 'Lê Hoàng Cường', 'An Toàn Thông Tin', 3.92, 1, UNIX_TIMESTAMP() * 1000, 'SERVER_1')
ON DUPLICATE KEY UPDATE name=VALUES(name);


-- ------------------------------------------------------------------------------
-- 2. CƠ SỞ DỮ LIỆU CHO SERVER 2
-- ------------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS db_server2 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_server2;

CREATE TABLE IF NOT EXISTS records (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    score DOUBLE NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    last_modified BIGINT NOT NULL,
    updated_by_server VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS sync_events (
    id VARCHAR(50) PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    record_id VARCHAR(50) NOT NULL,
    origin_server VARCHAR(50) NOT NULL,
    target_server VARCHAR(50) NOT NULL,
    description TEXT
);
