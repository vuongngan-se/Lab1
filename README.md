# TỔNG HỢP 5 BÀI LAB LẬP TRÌNH MẠNG (LTM) - VKU
**Sinh viên thực hiện:** Vương Lê Bảo Ngân  
**Mã số sinh viên (MSSV):** 24NS041  
**Trường:** Đại học Công nghệ Thông tin & Truyền thông Việt - Hàn (VKU) - Khoa Khoa Học Máy Tính

---

> **Dự án hoàn thành đầy đủ 5/5 bài Lab theo chuẩn yêu cầu môn học Lập trình mạng (LTM).**
> Hỗ trợ chạy đa nền tảng (**Windows `.bat`** và **Linux/macOS `.sh`**), tích hợp **Smart Dual-Mode MySQL Database** (tự động kết nối MySQL nếu có XAMPP/MySQL Server, tự động fallback sang tệp tin `.dat` an toàn tuyệt đối khi không có MySQL).

---

## 📁 Danh Mục Các Bài Lab & Thư Mục Nộp Bài

| STT | Thư Mục Dự Án | Tên Bài Lab | Giao Thức / Kỹ Thuật | Tính Năng & Cơ Sở Dữ Liệu | Script Chạy Nhanh |
|:---:|:---|:---|:---|:---|:---|
| **1** | `VuongLeBaoNgan_24NS041_Lab1_Chat_Client` | **Chat Multi-Client / P2P** | TCP Socket, UDP Multicast, UDP Broadcast, Voice/Video Call | • Giao diện Chat hiện đại Messenger/Zalo<br/>• Emoji, chia sẻ file, gọi thoại, video call<br/>• **Chạy mượt 5-6 Client đồng thời** | `run_server.bat`<br/>`run_5_clients.bat` |
| **2** | `VuongLeBaoNgan_24NS041_Lab2_FTP` | **FTP Server & FTP Client** | FTP (RFC 959), Active Mode & Passive Mode (PASV) | • **MySQL Database (`db_ftp`)** lưu tài khoản & logs<br/>• Giao diện Client 2 bảng chuẩn **FileZilla**<br/>• Tương thích 100% với FileZilla Client thật | `run_server.bat`<br/>`run_client.bat` |
| **3** | `VuongLeBaoNgan_24NS041_Lab3_Mail(Client)` | **Mail Server & Mail Client** | SMTP (RFC 5321), POP3 (RFC 1939), SSL/TLS | • **MySQL Database (`db_mail`)** lưu hòm thư & tài khoản<br/>• Giao diện Client hiện đại chuẩn Gmail<br/>• **Gửi Localhost & gửi qua Google Gmail thật** | `run_server.bat`<br/>`run_client.bat` |
| **4** | `VuongLeBaoNgan_24NS041_Lab4_WebServer` | **Web Server & Mini Browser** | HTTP/1.1, GET, HEAD, POST, Byte-Range (206) | • **MySQL Database (`db_web`)** lưu POST requests<br/>• Phục vụ **đa dịch vụ**: HTML, PDF, Nhạc MP3, Video MP4 Streaming, REST API<br/>• Trình duyệt Mini Browser có History & Download Manager | `run_server.bat`<br/>`run_browser.bat` |
| **5** | `VuongLeBaoNgan_24NS041_Lab5_RMI` | **RMI Đồng Bộ Hóa Dữ Liệu 2 Server** | Java RMI, Active-Active Replication, Heartbeat, Failover | • **MySQL Database (`vku_rmi_bank`)**<br/>• Đồng bộ hóa 2 chiều giữa 2 Server độc lập<br/>• Bảng điều khiển Cluster Monitor hiển thị dữ liệu nhất quán 100%<br/>• Chạy được trên 2 máy thực tế trong mạng LAN | `run_cluster.bat`<br/>`run_server1.bat`<br/>`run_server2.bat` |

---

## 🗄️ Cơ Chế Smart Dual-Mode MySQL Database (Lab 2, 3, 4, 5)

Mỗi bài Lab 2, 3, 4, 5 đều được trang bị tệp `database.sql` và thư viện JDBC driver MySQL (`lib/mysql-connector-j-8.3.0.jar`):
- **Chế độ MySQL Server:** Khi máy có cài đặt và khởi động MySQL (XAMPP / MySQL Service port `3306`, user: `root`, pass: `""`), ứng dụng sẽ tự động kết nối, tự động tạo CSDL (`db_ftp`, `db_mail`, `db_web`, `vku_rmi_bank`) cùng cấu trúc bảng dữ liệu nếu chưa có.
- **Chế độ Tự Động Fallback:** Khi MySQL không bật, ứng dụng tự động chuyển sang chế độ lưu trữ file `.dat` cục bộ, đảm bảo chương trình không bao giờ gặp lỗi Exception hay crash, người dùng và thầy cô có thể kiểm tra bài thi ngay lập tức mà không bắt buộc phải cài đặt cấu hình MySQL trước.

---

## 🚀 Hướng Dẫn Biên Dịch & Chạy Thử Nhanh

### 1. Trên Windows:
- **Biên dịch toàn bộ 5 bài Lab chỉ với 1 click:**
  ```cmd
  compile_all.bat
  ```
- **Chạy từng Lab:** Vào thư mục của Lab đó và nhấp đúp vào script `.bat` tương ứng (Ví dụ: `run_server.bat`, `run_client.bat`).

### 2. Trên Linux / Ubuntu / macOS:
- Cấp quyền thực thi và biên dịch toàn bộ 5 bài Lab:
  ```bash
  chmod +x *.sh */*.sh
  ./compile_all.sh
  ```
- Khởi động từng Lab bằng script `.sh` tương ứng:
  ```bash
  ./VuongLeBaoNgan_24NS041_Lab4_WebServer/run_server.sh
  ```
