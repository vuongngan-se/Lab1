-- ==============================================================================
-- KỊCH BẢN TẠO CƠ SỞ DỮ LIỆU MYSQL CHO LAB 4: WEB SERVER (HTTP/1.1) - VKU LTM
-- ==============================================================================
-- Hệ thống hỗ trợ chế độ Smart Dual-Mode:
-- - Tự động tạo cơ sở dữ liệu `db_web` và các bảng nếu MySQL đang chạy.
-- - Tự động lưu trữ các yêu cầu HTTP POST từ Browser vào MySQL.
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS db_web CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE db_web;

-- Bảng lưu trữ dữ liệu các yêu cầu HTTP POST gửi từ Trình duyệt (Form Data / API)
CREATE TABLE IF NOT EXISTS web_posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    timestamp BIGINT NOT NULL,
    client_ip VARCHAR(50) NOT NULL,
    raw_data TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng danh mục các dịch vụ đa phương tiện của Web Server
CREATE TABLE IF NOT EXISTS web_services (
    id INT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    mime_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    description TEXT
);

-- Dữ liệu mẫu ban đầu cho danh mục dịch vụ
INSERT INTO web_services (service_name, mime_type, file_path, description) VALUES
('Trang Chủ Web Portal', 'text/html', 'index.html', 'Giao diện chính giới thiệu hệ thống LTM'),
('Tài Liệu Học Phần PDF', 'application/pdf', 'sample.pdf', 'Tài liệu hướng dẫn môn Lập Trình Mạng'),
('Nhạc Phát Trực Tuyến MP3', 'audio/mpeg', 'sample.mp3', 'Dịch vụ phát âm thanh streaming'),
('Video Streaming MP4', 'video/mp4', 'sample.mp4', 'Dịch vụ video hỗ trợ tua HTTP 206 Partial Content'),
('REST API Thống Kê', 'application/json', '/api/stats', 'Dịch vụ cung cấp dữ liệu JSON từ MySQL')
ON DUPLICATE KEY UPDATE service_name=VALUES(service_name);
