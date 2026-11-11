-- ==============================================================================
-- KỊCH BẢN TẠO CƠ SỞ DỮ LIỆU MYSQL CHO LAB 2: FTP SERVER (RFC 959) - VKU LTM
-- ==============================================================================
-- Hệ thống hỗ trợ chế độ Smart Dual-Mode:
-- - Tự động tạo cơ sở dữ liệu `db_ftp` và các bảng nếu MySQL đang chạy.
-- - Tự động nạp dữ liệu từ MySQL hoặc đồng bộ 2 chiều với bộ nhớ máy chủ.
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS db_ftp CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_ftp;

-- Bảng quản lý người dùng FTP (Phân quyền RBAC)
CREATE TABLE IF NOT EXISTS ftp_users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    home_directory VARCHAR(255) NOT NULL,
    can_write BOOLEAN NOT NULL DEFAULT TRUE,
    can_delete BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- Bảng lưu nhật ký kiểm toán FTP Server (Audit Logs)
CREATE TABLE IF NOT EXISTS ftp_logs (
    id VARCHAR(50) PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    client_ip VARCHAR(50) NOT NULL,
    username VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_path VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    bytes_transferred BIGINT NOT NULL DEFAULT 0
);

-- Dữ liệu mẫu ban đầu
INSERT INTO ftp_users (username, password, home_directory, can_write, can_delete, enabled) VALUES
('admin', 'admin123', 'ftp_root', TRUE, TRUE, TRUE),
('vku', '123456', 'ftp_root', TRUE, FALSE, TRUE),
('anonymous', 'guest', 'ftp_root/public', FALSE, FALSE, TRUE)
ON DUPLICATE KEY UPDATE password=VALUES(password);

