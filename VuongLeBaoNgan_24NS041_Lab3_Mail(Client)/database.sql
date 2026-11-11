-- ==============================================================================
-- KỊCH BẢN TẠO CƠ SỞ DỮ LIỆU MYSQL CHO LAB 3: MAIL SERVER (SMTP & POP3) - VKU LTM
-- ==============================================================================
-- Hệ thống hỗ trợ chế độ Smart Dual-Mode:
-- - Tự động tạo cơ sở dữ liệu `db_mail` và các bảng nếu MySQL đang chạy.
-- - Tự động nạp dữ liệu từ MySQL hoặc đồng bộ 2 chiều với bộ nhớ máy chủ.
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS db_mail CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_mail;

-- Bảng quản lý tài khoản hòm thư
CREATE TABLE IF NOT EXISTS mail_accounts (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL
);

-- Bảng lưu trữ thư điện tử (Inbox / Sent)
CREATE TABLE IF NOT EXISTS mail_messages (
    id VARCHAR(50) PRIMARY KEY,
    sender VARCHAR(100) NOT NULL,
    recipient VARCHAR(100) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body TEXT,
    attachment_name VARCHAR(255),
    timestamp BIGINT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- Bảng lưu nhật ký giao thức SMTP và POP3
CREATE TABLE IF NOT EXISTS mail_logs (
    id VARCHAR(50) PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    protocol VARCHAR(20) NOT NULL,
    client_ip VARCHAR(50) NOT NULL,
    details TEXT,
    status VARCHAR(20) NOT NULL
);

-- Dữ liệu mẫu ban đầu
INSERT INTO mail_accounts (username, password, email, full_name) VALUES
('alice', '123456', 'alice@vku.udn.vn', 'Alice Nguyễn'),
('bob', '123456', 'bob@vku.udn.vn', 'Bob Trần'),
('charlie', '123456', 'charlie@vku.udn.vn', 'Charlie Lê'),
('admin', 'admin123', 'admin@localhost', 'Quản Trị Viên')
ON DUPLICATE KEY UPDATE password=VALUES(password);

INSERT INTO mail_messages (id, sender, recipient, subject, body, attachment_name, timestamp, is_read, is_deleted) VALUES
('MAIL-WELCOME-01', 'admin@localhost', 'alice@vku.udn.vn', 'Chào mừng bạn đến với VKU Mail Server', 'Xin chào Alice,\n\nHệ thống Mail Server Lập Trình Mạng hỗ trợ đầy đủ giao thức SMTP (gửi thư cổng 2525) và POP3 (nhận thư cổng 1100).\nChúc bạn thực hiện tốt bài Lab!\n\nTrân trọng,\nAdmin VKU', NULL, UNIX_TIMESTAMP() * 1000, FALSE, FALSE)
ON DUPLICATE KEY UPDATE subject=VALUES(subject);

