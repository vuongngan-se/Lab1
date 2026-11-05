# BÁO CÁO VÀ HƯỚNG DẪN LAB 3: MAIL SERVER & MAIL CLIENT (POP3 & SMTP)

## 1. Yêu Cầu Đề Bài & Mức Độ Hoàn Thiện
- **Giao thức hỗ trợ:**
  - **SMTP (Simple Mail Transfer Protocol - RFC 5321):**
    - Cổng máy chủ: `2525` (tránh xung đột cổng 25 bị các ISP chặn).
    - Hỗ trợ các lệnh chuẩn: `HELO`, `EHLO`, `MAIL FROM:<...>`, `RCPT TO:<...>`, `DATA` (nhận headers Subject, From, To, Date, attachment và kết thúc bằng `\r\n.\r\n`), `RSET`, `QUIT`.
    - Trả lời mã chuẩn: `220 ...`, `250 OK`, `354 Start mail input`, `221 Bye`.
  - **POP3 (Post Office Protocol version 3 - RFC 1939):**
    - Cổng máy chủ: `1100`.
    - Hỗ trợ các lệnh: `USER`, `PASS`, `STAT` (kiểm tra số lượng thư và tổng octets), `LIST`, `RETR` (tải nội dung thư chuẩn RFC 822), `DELE` (xóa thư), `RSET`, `QUIT`.
- **Máy chủ có Cơ sở dữ liệu (Database):**
  - Quản lý tài khoản hộp thư (`MailAccount`): `alice@vku.udn.vn`, `bob@vku.udn.vn`, `charlie@vku.udn.vn`, `admin@localhost`.
  - Lưu trữ hòm thư trung tâm (`MailMessage`): ID thư, người gửi, người nhận, tiêu đề, ngày giờ, nội dung, tệp đính kèm.
  - Lưu trữ nhật ký giao dịch SMTP/POP3 (`MailLog`).
- **Gửi được thư trên Gmail và trên Localhost:**
  - **Gửi trên Localhost:** Kết nối tới Mail Server tự tạo qua cổng 2525 và tải thư qua cổng 1100.
  - **Gửi qua Google Gmail thật:** Hỗ trợ kết nối SSL socket tới `smtp.gmail.com:465` xác thực bằng App Password (mật khẩu ứng dụng của Google) và tải thư qua `pop.gmail.com:995` SSL.
- **Giao diện tương tự Gmail:**
  - Thiết kế hiện đại với nút "+ Soạn Thư Mới" nổi bật, Hộp thư đến (Inbox), Xem chi tiết thư, Tải file đính kèm, Chuyển đổi linh hoạt giữa tài khoản Localhost và Gmail.

---

## 2. Tài Khoản Mặc Định Trong Database (Mật khẩu: `123456`)
1. `alice` -> `alice@vku.udn.vn`
2. `bob` -> `bob@vku.udn.vn`
3. `charlie` -> `charlie@vku.udn.vn`
4. `admin` -> `admin@localhost` (Mật khẩu: `admin123`)

---

## 3. Cách Khởi Động & Chấm Điểm
1. **Khởi động Mail Server:** Chạy `run_server.bat`.
   - Xem bảng điều khiển hiển thị trạng thái lắng nghe cổng SMTP 2525 và POP3 1100.
   - Tab "Dữ Liệu Thư Trong Database" hiển thị các thư hiện có trên máy chủ.
2. **Khởi động Mail Client:** Chạy `run_client.bat`.
   - Bấm nút **"+ Soạn Thư Mới"** để gửi thư từ `alice@vku.udn.vn` đến `bob@vku.udn.vn`.
   - Nhập tiêu đề, nội dung, đính kèm tệp và bấm **"Gửi Thư (Send)"**.
   - Trên Mail Server, tab Logs và Database lập tức ghi nhận email mới vừa được lưu trữ.
   - Đổi tài khoản sang `bob@vku.udn.vn` hoặc mở thêm một client để kiểm tra hộp thư đến của Bob đã nhận được thư.
3. **Thử nghiệm gửi qua Google Gmail thật (Gửi email thực tế ra Internet):**
   - **Bước 1 (Lấy Mật khẩu ứng dụng 16 ký tự của Google):**
     - Đăng nhập tài khoản Google của bạn tại: https://myaccount.google.com/
     - Vào mục **Bảo mật (Security)** -> Bật **Xác minh 2 bước (2-Step Verification)** (nếu chưa bật).
     - Truy cập trang tạo mật khẩu ứng dụng: https://myaccount.google.com/apppasswords
     - Đặt tên (ví dụ: `VKU Mail Client`) rồi bấm **Tạo (Create)**.
     - Google sẽ cung cấp 1 mã 16 chữ cái (dạng: `xxxx xxxx xxxx xxxx`). Sao chép mã này.
   - **Bước 2 (Cấu hình trên VKU Gmail Client):**
     - Mở client bằng `run_client.bat` -> Vào menu **"⚙ Đổi Tài Khoản / Gmail"**.
     - Chọn chế độ: `2. Google Gmail Thật (smtp.gmail.com:465 & pop.gmail.com:995)`.
     - Nhập địa chỉ Gmail của bạn và dán **Mật khẩu ứng dụng 16 ký tự** vừa tạo vào ô Mật khẩu. Bấm **OK**.
   - **Bước 3 (Gửi & Nhận thư thật):**
     - Bấm **"+ Soạn Thư Mới"** -> Gửi tới một địa chỉ email thật bất kỳ (ví dụ: email sinh viên hoặc email khác của bạn).
     - Bấm **"Gửi Thư (Send)"**.
     - Mở hòm thư Gmail trên trình duyệt/điện thoại hoặc bấm **"📋 Nhật Ký Giao Thức"** trên ứng dụng để xem quá trình bắt tay SSL và gửi lệnh SMTP tới Google.

---

## 4. Hướng Dẫn Chi Tiết Kiểm Thử Trên 2 Máy Tính Khác Nhau (Mạng LAN / Wi-Fi)

### 4.1. Mô hình triển khai (Topology)
- **Máy 1 (Máy Chủ Mail Server & Client Alice):**
  - Chạy `run_server.bat` (Lắng nghe cổng SMTP `2525` và POP3 `1100` trên toàn bộ card mạng `0.0.0.0`).
  - Chạy `run_client.bat` đăng nhập tài khoản: `alice@vku.udn.vn` (kết nối `127.0.0.1` hoặc IP Máy 1).
- **Máy 2 (Client Bob):**
  - Chạy `run_client.bat` đăng nhập tài khoản: `bob@vku.udn.vn`.
  - Cấu hình trỏ Server IP về địa chỉ IPv4 của **Máy 1** (ví dụ: `192.168.1.15`).

---

### 4.2. Bước 1: Chuẩn bị mạng LAN hoặc Wi-Fi chung
1. Kết nối cả 2 máy tính vào **cùng một mạng Wi-Fi** (hoặc cắm chung dây mạng LAN / Switch).
2. *Lưu ý mạng công cộng/trường học:* Một số mạng Wi-Fi công cộng bật tính năng *AP Client Isolation* (ngăn các máy trạm thấy nhau). Nếu gặp tình huống này, hãy bật **Mobile Hotspot** từ điện thoại hoặc từ Máy 1 để cả 2 máy cùng kết nối vào.

---

### 4.3. Bước 2: Xác định IP Máy 1 và mở Tường Lửa (Windows Firewall)
1. **Lấy địa chỉ IPv4 của Máy 1:**
   - **Cách 1 (Trực quan nhất):** Khởi chạy `run_server.bat` trên Máy 1. IP mạng LAN của máy chủ sẽ được tự động nhận diện và hiển thị trực tiếp trên góc phải thanh tiêu đề: `IP Máy Chủ: 192.168.x.x`.
   - **Cách 2:** Mở Terminal/cmd trên Máy 1, gõ `ipconfig` -> Tìm mục `Wireless LAN adapter Wi-Fi` hoặc `Ethernet adapter`, ghi lại `IPv4 Address` (ví dụ: `192.168.1.15`).
2. **Mở cổng Tường Lửa (Firewall) trên Máy 1:**
   - Khi chạy Server lần đầu, nếu Windows Defender Firewall xuất hiện popup hỏi quyền, hãy **tick chọn cả Private và Public networks** rồi bấm **Allow access**.
   - Nếu Máy 2 báo lỗi không kết nối được, mở nhanh 2 cổng `2525` và `1100` bằng cách mở **PowerShell (Run as Administrator)** trên Máy 1 và chạy lệnh sau:
     ```powershell
     New-NetFirewallRule -DisplayName "VKU Mail Server" -Direction Inbound -LocalPort 2525,1100 -Protocol TCP -Action Allow
     ```
3. **Kiểm tra thông mạng:** Trên Máy 2, mở cmd gõ `ping <IP_Máy_1>` (ví dụ `ping 192.168.1.15`). Nếu thấy `Reply from ...` là 2 máy đã thông nhau.

---

### 4.4. Bước 3: Copy thư mục dự án sang Máy 2 & Khởi động Client
1. Sao chép toàn bộ thư mục `lab3` sang **Máy 2** (hoặc chỉ cần thư mục `bin` và file `run_client.bat`).
2. Trên Máy 2, khởi chạy:
   ```cmd
   run_client.bat
   ```
3. Cấu hình kết nối về Máy 1:
   - Trên giao diện VKU Gmail của Máy 2, click vào nút **"⚙ Đổi Tài Khoản / Gmail"** ở thanh điều hướng bên trái.
   - Nhập thông tin:
     - **Môi trường gửi/nhận:** `1. Máy Chủ Cục Bộ / LAN (SMTP:2525 & POP3:1100)`
     - **Địa chỉ IP Máy Chủ (Server IP):** Nhập IP của Máy 1 (ví dụ: `192.168.1.15`)
     - **Email / Tên tài khoản:** `bob@vku.udn.vn`
     - **Mật khẩu:** `123456`
   - Bấm **OK**. Góc dưới sidebar sẽ hiển thị: `Đang đăng nhập: bob@vku.udn.vn (Máy Chủ: 192.168.1.15)`.

---

### 4.5. Các Kịch Bản Kiểm Thử Chấm Điểm (Test Cases)
- **Test Case 1: Máy 1 (Alice) gửi email kèm tệp đính kèm sang Máy 2 (Bob)**
  1. Trên Máy 1 (Alice), bấm **"+ Soạn Thư Mới"**.
  2. Điền:
     - Đến (To): `bob@vku.udn.vn`
     - Tiêu đề: `Xin chào Bob từ Máy 1`
     - Nội dung: `Đây là email thử nghiệm gửi qua mạng LAN từ Máy 1 sang Máy 2.`
     - Đính kèm tệp: Bấm `📎 Đính Kèm Tệp...` chọn 1 file ảnh hoặc tài liệu bất kỳ.
  3. Bấm **"Gửi Thư (Send)"** -> Báo gửi thành công.
  4. Quan sát trên **Mail Server GUI (Máy 1)**:
     - Tab *Nhật Ký Giao Thức*: Ghi lại chu trình lệnh SMTP chuẩn RFC 5321 (`HELO`, `MAIL FROM`, `RCPT TO`, `DATA`, `QUIT`).
     - Tab *Dữ Liệu Thư Trong Database*: Xuất hiện ngay email gửi đến `bob@vku.udn.vn`.
  5. Trên **Máy 2 (Bob)**: Bấm nút **"🔄 Làm Mới"** -> Email từ Alice lập tức xuất hiện trong Hộp thư đến! Click vào thư để xem nội dung chi tiết và bấm **"💾 Tải Tệp Đính Kèm"** để lưu file về máy.
- **Test Case 2: Máy 2 (Bob) gửi phản hồi ngược lại cho Máy 1 (Alice)**
  1. Trên Máy 2 (Bob), bấm **"+ Soạn Thư Mới"**.
  2. Nhập Người nhận: `alice@vku.udn.vn`, tiêu đề: `Bob đã nhận được thư từ Máy 2!`, nội dung tùy ý.
  3. Bấm **"Gửi Thư (Send)"**.
  4. Trên Server Máy 1: Ghi nhận log SMTP kết nối trực tiếp từ địa chỉ IP của Máy 2.
  5. Trên Máy 1 (Alice): Bấm **"🔄 Làm Mới"** -> Hộp thư Alice lập tức nhận được phản hồi từ Bob.
- **Test Case 3: Tra cứu nhật ký giao thức (Protocol Logs)**
  - Bấm vào mục **"📋 Nhật Ký Giao Thức"** trên cả 2 máy để xem chi tiết các lệnh POP3 (`USER`, `PASS`, `STAT`, `RETR`, `QUIT`) và SMTP theo thời gian thực.

---

### 4.6. Xử lý sự cố thường gặp (Troubleshooting)
| Hiện Tượng | Nguyên Nhân | Cách Khắc Phục |
| :--- | :--- | :--- |
| **Connection refused** | Server chưa chạy hoặc nhập sai cổng | Đảm bảo `run_server.bat` đã mở trên Máy 1; kiểm tra cổng SMTP `2525`, POP3 `1100`. |
| **Connection timed out / Treo kết nối** | Firewall trên Máy 1 đang chặn cổng | Chạy lệnh mở cổng bằng PowerShell (Admin) ở mục 4.3 hoặc tạm thời tắt Firewall trên Máy 1. |
| **Không ping được IP Máy 1** | Khác mạng Wi-Fi hoặc Wi-Fi bật Isolation | Dùng Máy 1 bật Mobile Hotspot phát Wi-Fi cho Máy 2 kết nối vào. |

---

## 5. Câu Hỏi Giáo Viên Thường Hỏi
### Q1: SMTP và POP3 khác nhau như thế nào?
- **SMTP (Simple Mail Transfer Protocol - Cổng chuẩn 25 / 465 / 587):**
  - Là giao thức **đẩy (Push Protocol)**, dùng để truyền gửi thư từ Mail Client lên Mail Server hoặc giữa các Mail Server với nhau.
- **POP3 (Post Office Protocol - Cổng chuẩn 110 / 995):**
  - Là giao thức **kéo (Pull Protocol)**, dùng để Mail Client kết nối vào máy chủ hộp thư để tải/đọc thư về máy tính cá nhân.

### Q2: Tại sao kết thúc phần DATA trong SMTP lại dùng dấu chấm trên một dòng riêng (`\r\n.\r\n`)?
- Vì phần thân thư (`DATA`) có thể chứa nhiều dòng văn bản tùy ý. Để máy chủ biết chính xác khi nào người dùng đã hoàn thành việc gửi nội dung email mà không cần đóng kết nối TCP (để còn gửi lệnh `QUIT` hoặc gửi tiếp thư khác), chuẩn RFC 5321 quy định chuỗi thoát kết thúc là dấu chấm nằm đơn độc trên một dòng riêng biệt.
