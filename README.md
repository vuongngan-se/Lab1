# TỔNG HỢP 5 BÀI LAB LẬP TRÌNH MẠNG (LTM) - VKU

> **Dự án hoàn thành đầy đủ 5/5 bài Lab theo yêu cầu đề bài môn học Lập trình mạng - Trường Đại học Công nghệ Thông tin và Truyền thông Việt - Hàn (VKU).**
> Mỗi bài Lab được tổ chức độc lập trong một thư mục riêng biệt (`lab1`, `lab2`, `lab3`, `lab4`, `lab5`), có mã nguồn Java hoàn chỉnh, file cấu hình Maven `pom.xml`, các script thực thi `.bat` tự động biên dịch và chạy bằng 1 cú nhấp chuột, cùng tài liệu hướng dẫn và câu hỏi vấn đáp cho từng Lab.

---

## Danh Mục Các Bài Lab & Thư Mục

| STT | Thư Mục | Tên Lab | Giao Thức & Kỹ Thuật | Yêu Cầu Đã Hoàn Thiện | Script Chạy Nhanh |
|:---:|:---:|:---|:---|:---|:---|
| **1** | [lab1/](file:///d:/VKU/VKU/5/LTM/Project/Lab/lab1/) | **Chat App (Client-Server / P2P)** | TCP Socket, UDP Multicast (`224.2.2.3`), UDP Broadcast, UDP Media Stream | • Giao diện Messenger/Zalo<br/>• Multicast & Broadcast<br/>• Bảng chọn Icon cảm xúc (Emoji)<br/>• Text, Voice Audio, Video Call<br/>• Gửi tệp đính kèm<br/>• Database lưu trữ tài khoản & lịch sử<br/>• **Chạy từ 5-6 Client đồng thời** | `lab1\run_server.bat`<br/>`lab1\run_5_clients.bat` |
| **2** | [lab2/](file:///d:/VKU/VKU/5/LTM/Project/Lab/lab2/) | **FTP Server & FTP Client** | FTP (RFC 959), PASV, PORT, Command & Data Sockets | • Đầy đủ lệnh RFC 959 chuẩn<br/>• Chế độ Passive Mode & Active Mode<br/>• **Server có Database** quản lý tài khoản & Audit Logs<br/>• Giao diện Client 2 bảng tương tự **FileZilla**<br/>• Tương thích 100% với FileZilla Client thật | `lab2\run_server.bat`<br/>`lab2\run_client.bat` |
| **3** | [lab3/](file:///d:/VKU/VKU/5/LTM/Project/Lab/lab3/) | **Mail Server & Mail Client** | SMTP (RFC 5321 - Port 2525), POP3 (RFC 1939 - Port 1100), SSL/TLS | • Mail Server có Database lưu hòm thư và tài khoản<br/>• Giao diện Client hiện đại tương tự **Gmail**<br/>• Soạn thư, đính kèm file, đọc thư<br/>• **Gửi được trên Localhost & gửi qua Google Gmail thật** (SSL Port 465) | `lab3\run_server.bat`<br/>`lab3\run_client.bat` |
| **4** | [lab4/](file:///d:/VKU/VKU/5/LTM/Project/Lab/lab4/) | **Web Server & Mini Browser** | HTTP/1.1, GET, HEAD, POST, Byte-Range (206 Partial Content) | • Web Server phục vụ **hơn 1 dịch vụ** (.html, tài liệu .pdf, nhạc .mp3, video .mp4 streaming)<br/>• Mini Browser giao diện Chrome/Cốc Cốc<br/>• Hỗ trợ các phương thức **GET, HEAD, POST**<br/>• Lịch sử duyệt web (**History**)<br/>• Trình quản lý tải xuống (**Download Manager**) có tiến độ % | `lab4\run_server.bat`<br/>`lab4\run_browser.bat` |
| **5** | [lab5/](file:///d:/VKU/VKU/5/LTM/Project/Lab/lab5/) | **RMI Đồng Bộ Hóa Dữ Liệu 2 Server** | Java RMI (`java.rmi.*`), Active-Active Replication, Heartbeat, Failover | • **Đồng bộ hóa 2 chiều trên 2 Server**<br/>• Hỗ trợ cấu hình IP chạy trên **2 máy thực tế**<br/>• Chạy nhiều Client trên các máy ảo<br/>• Bảng điều khiển Cluster Monitor hiển thị dữ liệu nhất quán 100%<br/>• Tự động phục hồi & Catch-up Sync khi kết nối lại<br/>• Tự động chuyển đổi máy chủ (Failover) khi 1 server gặp sự cố | `lab5\run_cluster.bat`<br/>(Khởi động cụm 2 Server + Monitor + Client) |

---

## Hướng Dẫn Biên Dịch & Chạy Thử Nhanh

### Yêu Cầu Môi Trường:
- Máy tính đã cài đặt **Java JDK 17** (Đã có sẵn trên máy: `C:\Program Files\Java\jdk-17\bin\javac.exe`).
- Không cần cài đặt thêm bất kỳ thư viện bên ngoài nào, tất cả đều chạy độc lập và ổn định 100% offline.

### Cách Chạy Nhanh Toàn Bộ Dự Án:
1. **Biên dịch toàn bộ 5 bài Lab cùng một lúc:**
   - Chạy file: `compile_all.bat`
2. **Chạy từng Lab theo hướng dẫn chi tiết trong từng thư mục:**
   - Xem file `README.md` tại mỗi thư mục `lab1/`, `lab2/`, `lab3/`, `lab4/`, `lab5/` để biết thêm chi tiết về câu hỏi vấn đáp và kịch bản chấm điểm của giảng viên.
