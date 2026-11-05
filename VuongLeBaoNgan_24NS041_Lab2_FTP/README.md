# BÁO CÁO VÀ HƯỚNG DẪN LAB 2: FTP SERVER & FTP CLIENT (RFC 959)

## 1. Yêu Cầu Đề Bài & Mức Độ Hoàn Thiện
- **Giao thức FTP chuẩn (RFC 959):** Triển khai đầy đủ hệ thống FTP bao gồm cả **FTP Server** và **FTP Client** giao diện đồ họa.
- **Lệnh FTP hỗ trợ:**
  - `USER`, `PASS`: Xác thực người dùng và phân quyền.
  - `PWD`: In đường dẫn thư mục hiện tại.
  - `CWD`, `CDUP`: Di chuyển giữa các thư mục và lên thư mục cha.
  - `TYPE I`: Thiết lập chế độ truyền nhị phân (Binary mode) đảm bảo tính toàn vẹn của tệp tin.
  - `PASV`: Chế độ Passive Mode, máy chủ mở cổng dữ liệu động và phản hồi mã `227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)`.
  - `PORT`: Chế độ Active Mode.
  - `LIST`: Trả về danh sách tệp/thư mục định dạng Unix chuẩn tương thích 100% với FileZilla.
  - `RETR`: Tải tệp từ máy chủ về máy khách (Download).
  - `STOR`: Tải tệp từ máy khách lên máy chủ (Upload).
  - `DELE`: Xóa tệp tin trên máy chủ (kiểm tra quyền `canDelete`).
  - `MKD`: Tạo thư mục mới trên máy chủ.
  - `RMD`: Xóa thư mục.
  - `SIZE`: Kiểm tra dung lượng tệp.
  - `QUIT`: Ngắt kết nối.
- **Máy chủ có Cơ sở dữ liệu (Database):**
  - Quản lý tài khoản người dùng (`FtpUser`), mật khẩu, thư mục gốc (`homeDirectory`), quyền ghi (`canWrite`), quyền xóa (`canDelete`).
  - Lưu trữ toàn bộ nhật ký kiểm toán (`FtpLog`) gồm: Thời gian, IP client, Tên đăng nhập, Thao tác thực hiện (LOGIN, LIST, DOWNLOAD, UPLOAD, DELETE, MKD), Tên tệp, Trạng thái (SUCCESS/FAILED) và Số byte đã truyền.
- **Giao diện tương tự FileZilla:**
  - Thiết kế bảng đôi (Dual-pane): Cây thư mục cục bộ (Local Site) bên trái và Cây thư mục máy chủ (Remote Site) bên phải.
  - Hỗ trợ tải lên (Upload), tải về (Download), xóa, tạo thư mục, làm mới và xem nhật ký trao đổi lệnh/phản hồi trực tiếp.
  - **Tương thích FileZilla thật:** Có thể dùng phần mềm FileZilla Client chính thức kết nối vào `127.0.0.1:2121`.

---

## 2. Tài Khoản Thử Nghiệm Có Sẵn Trong Database
1. `admin` / `admin123` (Toàn quyền Upload và Delete, thư mục `ftp_root`).
2. `vku` / `123456` (Quyền Upload, không có quyền Delete).
3. `anonymous` / `guest` (Chỉ đọc trong thư mục `ftp_root/public`).

---

## 3. Cách Khởi Động & Chấm Điểm
1. **Khởi động FTP Server:** Chạy `run_server.bat`. Giao diện quản trị sẽ mở ra:
   - Tab 1: Nhật ký lệnh thời gian thực.
   - Tab 2: Quản lý người dùng trong Database (Thêm/xóa user, phân quyền).
   - Tab 3: Nhật ký hoạt động trong Database.
2. **Khởi động FTP Client:** Chạy `run_client.bat`.
   - Bấm nút **"Kết Nối Nhanh"** (mặc định cấu hình `127.0.0.1:2121` với tài khoản `admin`).
   - Thao tác Upload tệp từ bên trái sang bên phải.
   - Thao tác Download tệp từ bên phải về máy.
   - Kiểm tra xem nhật ký trên Server và Database đã ghi nhận chính xác tên tệp và số byte truyền tải.

---

## 4. Hướng Dẫn Thử Nghiệm Trên 2 Máy Tính (2 Laptops / PCs)

Hệ thống hỗ trợ chạy mô hình Client - Server phân tán trên 2 máy tính vật lý khác nhau trong cùng mạng cục bộ (LAN / Wi-Fi).

```
   ┌───────────────────────────────┐              ┌───────────────────────────────┐
   │    MÁY 1 (FTP SERVER)         │   Wi-Fi/LAN  │    MÁY 2 (FTP CLIENT)         │
   │  - IPv4: 192.168.1.15 (ví dụ) │ <══════════> │  - Chạy VKU FTP Client        │
   │  - Port lắng nghe: 2121       │   TCP/IP     │    (hoặc FileZilla Client)    │
   │  - Quản lý Database & Root    │              │  - Kết nối tới 192.168.1.15   │
   └───────────────────────────────┘              └───────────────────────────────┘
```

### Bước 1: Chuẩn bị kết nối mạng giữa 2 máy
- Cả 2 máy phải nhìn thấy nhau trong cùng mạng. Có 2 cách thiết lập:
  - **Cách 1 (Khuyên dùng khi ở lớp học/thuyết trình):** Sử dụng **Mobile Hotspot** (Phát Wi-Fi từ điện thoại hoặc một trong 2 máy phát Hotspot cho máy kia kết nối).
    > [!TIP]
    > Mạng Wi-Fi tại trường học hoặc nơi công cộng thường bật tính năng bảo mật *AP Isolation* (cách ly thiết bị), khiến các máy tính không thể gửi gói tin cho nhau. Sử dụng Mobile Hotspot cá nhân sẽ đảm bảo 100% kết nối thông suốt và ổn định.
  - **Cách 2:** Cả 2 máy cùng kết nối vào một mạng Wi-Fi gia đình hoặc cắm chung dây cáp mạng LAN.

---

### Bước 2: Thiết lập trên MÁY 1 (MÁY CHỦ - FTP SERVER)
1. **Lấy địa chỉ IPv4 của Máy 1:**
   - Trên Máy 1, mở **Command Prompt (cmd)** hoặc **PowerShell**, gõ lệnh:
     ```cmd
     ipconfig
     ```
   - Tìm card mạng đang kết nối (thường là `Wireless LAN adapter Wi-Fi` hoặc `Ethernet adapter`).
   - Ghi lại dòng **`IPv4 Address`** (Ví dụ: `192.168.1.15` hoặc `192.168.137.1`).
2. **Cấu hình Tường lửa (Windows Defender Firewall) - Rất quan trọng:**
   - *Lần đầu chạy:* Khi khởi động Server, nếu Windows hiển thị hộp thoại bảo mật *Windows Security Alert*, hãy tích chọn cả **Private networks** và bấm **Allow access**.
   - *Nếu Máy 2 không kết nối được:* Mở CMD với quyền Admin (`Run as administrator`) trên Máy 1 và chạy lệnh mở cổng `2121`:
     ```cmd
     netsh advfirewall firewall add rule name="VKU_FTP_SERVER_2121" dir=in action=allow protocol=TCP localport=2121
     ```
   - *(Hoặc tắt tạm thời Windows Defender Firewall cho Private network trong lúc test/demo).*
3. **Khởi động Server:**
   - Chạy file `run_server.bat` trên Máy 1.
   - Giao diện **VKU FTP Server Manager** sẽ xuất hiện với thông báo: `MÁY CHỦ FTP: Đang chạy trên cổng 2121`.

---

### Bước 3: Thiết lập & Kết nối từ MÁY 2 (MÁY KHÁCH - FTP CLIENT)

#### Cách A: Sử dụng giao diện VKU FTP Client (Khuyên dùng)
1. Copy thư mục dự án `lab2` (hoặc các file `run_client.bat`, thư mục `bin/`, `src/`) sang Máy 2.
2. Trên Máy 2, chạy file `run_client.bat`.
3. Điền thông tin kết nối trên thanh công cụ ở đỉnh giao diện:
   - **Host:** Nhập địa chỉ IPv4 của Máy 1 (Ví dụ: `192.168.1.15` — **tuyệt đối không để 127.0.0.1**).
   - **Port:** `2121`
   - **User:** `admin` (hoặc `vku`, `anonymous`)
   - **Pass:** `admin123` (hoặc `123456`, `guest`)
4. Bấm nút **"Kết Nối Nhanh"**.
5. Sau khi kết nối thành công:
   - Bảng bên phải (**Remote Site**) sẽ tải về toàn bộ danh mục tệp và thư mục trên Máy 1.
   - Hộp nhật ký phía dưới hiển thị chi tiết tiến trình gửi/nhận lệnh RFC 959 giữa 2 máy.

#### Cách B: Sử dụng phần mềm FileZilla Client chính thức
1. Cài đặt và mở **FileZilla Client** trên Máy 2.
2. Điền thông tin:
   - **Host:** `192.168.1.15` (IPv4 của Máy 1)
   - **Username:** `admin`
   - **Password:** `admin123`
   - **Port:** `2121`
3. Bấm **Quickconnect**. FileZilla sẽ bắt tay với Server trên Máy 1 thông qua giao thức chuẩn FTP.

---

### Bước 4: Kịch bản thử nghiệm chấm điểm giữa 2 máy
1. **Kiểm tra Upload (Máy 2 -> Máy 1):**
   - Trên Máy 2, chọn một file ở bảng trái (Local Site), bấm nút **"Tải Lên"** (hoặc chuột phải -> Tải lên).
   - Trên Máy 1, bấm nút **"Mở Thư Mục ftp_root"**: Kiểm tra file vừa gửi từ Máy 2 đã nằm trong thư mục của Máy 1 và mở ra xem nội dung nguyên vẹn.
2. **Kiểm tra Download (Máy 1 -> Máy 2):**
   - Trên Máy 2, chọn một file ở bảng phải (Remote Site), bấm nút **"Tải Về"**.
   - File sẽ được truyền qua mạng từ Máy 1 về lưu tại Máy 2 theo chế độ nhị phân (Binary mode).
3. **Kiểm tra phân quyền người dùng (Database RBAC):**
   - Ngắt kết nối, đăng nhập bằng tài khoản `vku` / `123456`:
     - Upload file: Thành công.
     - Xóa file: Bị từ chối (`550 Permission denied: User does not have delete permission`).
   - Đăng nhập bằng `anonymous` / `guest`:
     - Chỉ xem và tải file trong thư mục công cộng `public`. Không thể ghi, sửa hoặc xóa.
4. **Kiểm tra Giám sát trên Máy 1:**
   - Xem Tab **"Nhật Ký Thời Gian Thực"**: Hiển thị rõ địa chỉ IP thật của Máy 2 đang thực thi từng lệnh.
   - Xem Tab **"Nhật Ký Hoạt Động (Database)"**: Ghi nhận chính xác IP Máy 2, tài khoản đăng nhập, hành động (`UPLOAD`, `DOWNLOAD`, `DELE`...) và tổng số byte dữ liệu đã truyền.

---

### Bước 5: Hướng dẫn xử lý sự cố thường gặp (Troubleshooting)
| Hiện tượng | Nguyên nhân | Cách khắc phục |
| :--- | :--- | :--- |
| Máy 2 báo `Connection timed out` hoặc `Connection refused` | 2 máy không cùng dải mạng hoặc bị chặn ping | Mở cmd Máy 2 gõ `ping <IP_Máy_1>`. Nếu không có phản hồi, hãy chuyển sang dùng Mobile Hotspot từ điện thoại hoặc kiểm tra lại tường lửa Máy 1. |
| Đăng nhập thành công nhưng danh sách file Remote Site quay mãi / lỗi data socket | Tường lửa Máy 1 chặn cổng dữ liệu động (Passive data ports) | Vào Windows Defender Firewall trên Máy 1 tắt tạm thời Firewall mạng cá nhân (Private network), hoặc cho phép ứng dụng `java.exe` đi qua Firewall. |
| Báo lỗi `Sai thông tin đăng nhập` | Nhập nhầm chữ hoa/thường | Kiểm tra Caps Lock, tài khoản mẫu: `admin`/`admin123`, `vku`/`123456`. |

---

## 5. Câu Hỏi Giáo Viên Thường Hỏi (Vấn Đáp)
### Q1: FTP sử dụng mấy kết nối TCP?
- FTP sử dụng **2 kết nối TCP riêng biệt**:
  1. **Control Connection (Cổng lệnh 21 hoặc 2121):** Duy trì trong suốt phiên làm việc để gửi các lệnh FTP (USER, PASS, LIST, RETR, STOR...) và nhận mã phản hồi (220, 331, 230, 150, 226...).
  2. **Data Connection (Cổng dữ liệu):** Chỉ được mở ra khi cần truyền danh sách tệp hoặc nội dung tệp tin, sau khi truyền xong sẽ lập tức đóng lại.

### Q2: Chế độ Active Mode (PORT) và Passive Mode (PASV) khác nhau như thế nào?
- **Active Mode (PORT):** Client mở cổng dữ liệu tạm thời, gửi lệnh `PORT h1,h2,h3,h4,p1,p2` cho Server. Server chủ động khởi tạo kết nối TCP ngược lại tới Client. (Cách này thường bị tường lửa Firewall hoặc NAT phía Client chặn kết nối vào).
- **Passive Mode (PASV):** Client gửi lệnh `PASV`. Server mở cổng dữ liệu tạm thời và trả lời `227 Entering Passive Mode (...)`. Sau đó Client chủ động kết nối tới cổng dữ liệu của Server. (Cách này thân thiện với tường lửa và NAT, là chuẩn mặc định của các trình FTP hiện đại).

### Q3: Khi chạy trên 2 máy tính khác nhau, tại sao cần chú ý địa chỉ IP và Tường lửa (Firewall)?
- **Địa chỉ IP:** Không thể dùng `127.0.0.1` (Loopback chỉ dùng nội bộ 1 máy). Phải dùng IP mạng LAN của Máy 1 (như `192.168.x.x`) để gói tin định tuyến qua card mạng Wi-Fi/Ethernet sang Máy 2.
- **Tường lửa (Firewall):** Hệ điều hành Windows mặc định chặn các kết nối lạ từ bên ngoài đi vào cổng `2121` hoặc các cổng dữ liệu PASV ngẫu nhiên. Cần mở port hoặc cho phép Java đi qua Firewall trên Máy 1.

