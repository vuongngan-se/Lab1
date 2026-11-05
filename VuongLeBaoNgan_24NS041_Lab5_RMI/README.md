# BÁO CÁO VÀ HƯỚNG DẪN LAB 5: RMI ĐỒNG BỘ HÓA DỮ LIỆU TRÊN 2 SERVER (LTM - VKU)

---

## 1. Yêu Cầu Đề Bài & Mức Độ Hoàn Thiện
- **Công nghệ sử dụng:** Java RMI (Remote Method Invocation) chuẩn (`java.rmi.*`, `LocateRegistry`, `UnicastRemoteObject`).
- **Mô hình kiến trúc phân tán:**
  - **Server 1** và **Server 2**:
    - Hoạt động theo mô hình **Active-Active (Đồng bộ 2 chiều - 2-Way Synchronization)**.
    - Mỗi Server kết nối với **Cơ sở dữ liệu MySQL (XAMPP)** độc lập:
      - Máy 1 (Server 1) lưu tại database: `db_server1`
      - Máy 2 (Server 2) lưu tại database: `db_server2`
      - *(Hệ thống tích hợp sẵn driver MySQL `mysql-connector-j-8.3.0.jar` trong thư mục `lib/` và tự động tạo Database + Table khi chạy, không cần tạo thủ công)*.
  - **Cơ chế đồng bộ thời gian thực (Real-time Replication):**
    - Khi Client kết nối tới Server 1 và thực hiện Thêm/Sửa/Xóa bản ghi:
      1. Server 1 ghi nhận thay đổi vào database cục bộ và tăng số `version`.
      2. Server 1 lập tức gọi giao diện RMI `IDataSyncService.syncCreate()` / `syncUpdate()` / `syncDelete()` trên Server 2.
      3. Server 2 nhận yêu cầu qua RMI, cập nhật database của mình, và nhận biết mã `originServerId == "SERVER_1"` nên **không phản hồi ngược lại**, loại bỏ hoàn toàn hiện tượng lặp vô tận (Replication Loop).
    - Tương tự, nếu một Client khác thao tác trên Server 2, dữ liệu lập tức được đồng bộ tức thời ngược lại Server 1.
  - **Cơ chế tự phục hồi & Đồng bộ bù (Catch-Up Sync on Reconnect):**
    - 2 Server liên tục trao đổi gói tin Heartbeat mỗi 3 giây.
    - Nếu Server 2 bị tắt/mất mạng trong lúc Server 1 nhận nhiều bản ghi mới, Server 1 vẫn tiếp tục phục vụ người dùng bình thường.
    - Ngay khi Server 2 được bật trở lại, quá trình bắt tay tự động kích hoạt: Server 2 phát hiện sự kiện kết nối lại và gọi `syncAll()` để kéo toàn bộ các bản ghi bị thiếu về database của mình.
  - **Cơ chế chịu lỗi Client (Automatic Failover):**
    - Lớp `FailoverManager` trên Client tự động phát hiện nếu Server đang kết nối bị mất tín hiệu và tự động chuyển hướng các lệnh sang Server còn lại mà không làm gián đoạn trải nghiệm người dùng.

---

## 2. Danh Sách Các File Khởi Chạy Sẵn Trong Project

| Tên File | Chức Năng | Môi Trường Khuyên Dùng |
| :--- | :--- | :--- |
| `compile.bat` | Biên dịch toàn bộ mã nguồn Java vào thư mục `bin` | Chạy trước khi test |
| `run_cluster.bat` | Khởi động trọn gói: Server 1 + Server 2 + Monitor + Client | **Test nhanh trên 1 máy** |
| `run_server_lan.bat` | Hộp thoại tương tác khởi động Server (chọn Server 1 hoặc 2, nhập IP LAN) | **Test trên 2 máy thực tế** |
| `run_client_lan.bat` | Khởi động Client kết nối tới 2 Server qua IP LAN | **Test trên 2 máy thực tế / Máy ảo** |
| `run_monitor_lan.bat` | Khởi động Cluster Monitor theo dõi trạng thái 2 máy qua IP LAN | **Test trên 2 máy thực tế** |
| `run_server1.bat` | Chạy Server 1 (Cổng 1099, localhost) | Test từng thành phần |
| `run_server2.bat` | Chạy Server 2 (Cổng 1100, localhost) | Test từng thành phần |
| `run_client.bat` | Chạy Client (kết nối localhost 1099 & 1100) | Test từng thành phần |
| `run_multi_clients.bat`| Mở 4 Client cùng lúc mô phỏng nhiều máy trạm | Test tải / mô phỏng nhiều máy ảo |

---

## 3. HƯỚNG DẪN TEST TRÊN 2 MÁY THỰC TẾ (MẠNG LAN / WIFI)

### Bước 0: Bật MySQL trên XAMPP (Cả 2 máy)
1. Mở **XAMPP Control Panel** trên cả Máy 1 và Máy 2.
2. Nhấn nút **Start** ở module **MySQL** (cổng mặc định `3306`).
> **Tính năng tự động:** Khi Server Java khởi động, nó sẽ tự động kết nối và tạo database + bảng (`db_server1` trên Máy 1, và `db_server2` trên Máy 2). Bạn không cần phải import thủ công! (Nếu cần import thủ công, file script đã có sẵn tại `database.sql`).
> **Xem dữ liệu trực tiếp:** Mở trình duyệt vào `http://localhost/phpmyadmin` để xem bảng `records` và `sync_events` cập nhật theo thời gian thực!

### Bước 1: Chuẩn bị mạng và lấy địa chỉ IP của 2 máy
1. **Kết nối chung mạng:**
   - Cả 2 máy tính (Máy A và Máy B) phải cùng kết nối vào **cùng một mạng Wi-Fi** hoặc cắm chung dây mạng LAN.
   > **Mẹo hữu ích khi ở phòng Lab:** Nếu mạng Wi-Fi trường bật tính năng *AP Isolation* (chặn các máy nói chuyện với nhau), bạn chỉ cần dùng **1 máy tính bật Mobile Hotspot** (Điểm phát sóng di động trên Windows) rồi cho máy kia kết nối vào, hoặc dùng 1 điện thoại phát 4G cho cả 2 máy kết nối vào.

2. **Lấy địa chỉ IPv4 của từng máy:**
   - Trên mỗi máy, mở cửa sổ Command Prompt (`cmd`) hoặc `PowerShell`, gõ:
     ```cmd
     ipconfig
     ```
   - Tìm dòng **IPv4 Address** (ví dụ: `192.168.1.15` cho Máy 1, và `192.168.1.25` cho Máy 2).
   
3. **Kiểm tra thông mạng (Ping test):**
   - Từ Máy 1 gõ ping sang Máy 2: `ping 192.168.1.25`
   - Từ Máy 2 gõ ping sang Máy 1: `ping 192.168.1.15`
   - Nếu nhận được `Reply from ...: bytes=32 time=... TTL=...` là 2 máy đã thông nhau thành công.

---

### Bước 2: Cấu hình Tường lửa Windows (Windows Defender Firewall)
> **LƯU Ý QUAN TRỌNG:** Java RMI cần lắng nghe kết nối từ bên ngoài. Nếu tường lửa chặn, 2 máy sẽ không thể bắt tay đồng bộ.

Có 3 cách xử lý (chọn 1 trong 3):
- **Cách 1 (Nhanh nhất):** Khi chạy Server lần đầu, Windows sẽ hiện thông báo *"Windows Defender Firewall has blocked some features of Java"*. Bạn hãy **tích chọn cả 2 ô [Private networks] và [Public networks]**, sau đó nhấn **Allow access**.
- **Cách 2 (Mở cổng bằng 1 lệnh PowerShell):** Mở PowerShell bằng quyền Admin (**Run as Administrator**) và chạy lệnh sau trên cả 2 máy:
  ```powershell
  New-NetFirewallRule -DisplayName "Java RMI VKU" -Direction Inbound -Protocol TCP -LocalPort 1099,1100 -Action Allow
  ```
- **Cách 3 (Đơn giản nhất trong lúc chấm bài):** Vào *Control Panel -> Windows Defender Firewall -> Turn Windows Defender Firewall on or off*, tạm thời chọn **Turn off Windows Defender Firewall** cho Private network trong thời gian chấm điểm.

---

### Bước 3: Khởi chạy trên 2 máy thực tế

#### CÁCH A: SỬ DỤNG SCRIPT BATCH TƯƠNG TÁC (Khuyên dùng - Rất tiện lợi)

Copy toàn bộ thư mục bài Lab sang cả 2 máy (hoặc clone từ Git), sau đó:

1. **Trên MÁY 1 (Đóng vai trò SERVER_1):**
   - Chạy file: `run_server_lan.bat`
   - Chọn vai trò: Nhập `1` (SERVER_1)
   - Nhập IPv4 máy này: Nhập IP Máy 1 (ví dụ `192.168.1.15`)
   - Nhập IPv4 máy đối tác: Nhập IP Máy 2 (ví dụ `192.168.1.25`)
   - Cổng RMI máy này: Nhấn `Enter` (mặc định `1099`)
   - Cổng RMI máy đối tác: Nhấn `Enter` (mặc định `1099`)
   - Cửa sổ Server 1 xuất hiện và sẵn sàng đón nhận kết nối.

2. **Trên MÁY 2 (Đóng vai trò SERVER_2):**
   - Chạy file: `run_server_lan.bat`
   - Chọn vai trò: Nhập `2` (SERVER_2)
   - Nhập IPv4 máy này: Nhập IP Máy 2 (ví dụ `192.168.1.25`)
   - Nhập IPv4 máy đối tác: Nhập IP Máy 1 (ví dụ `192.168.1.15`)
   - Cổng RMI máy này: Nhấn `Enter` (mặc định `1099`)
   - Cổng RMI máy đối tác: Nhấn `Enter` (mặc định `1099`)
   - Cửa sổ Server 2 xuất hiện, ngay lập tức bắt tay kết nối với Server 1!

3. **Khởi động Client và Monitor để thao tác & theo dõi:**
   - Bạn có thể bật Client trên Máy 1, Máy 2 hoặc một Máy Ảo thứ 3:
     - Chạy `run_client_lan.bat`: Nhập IP Server 1 và IP Server 2.
     - Chạy `run_monitor_lan.bat`: Nhập IP Server 1 và IP Server 2 để mở giao diện Giám sát Cụm (Cluster Monitor) trực quan.

---

#### CÁCH B: KHỞI CHẠY TRỰC TIẾP BẰNG DÒNG LỆNH (Dành cho báo cáo kỹ thuật)

Biên dịch trước trên mỗi máy: `compile.bat` (hoặc `javac -encoding UTF-8 -d bin src/com/vku/rmi/*/*.java`).

Giả sử:
- **Máy 1 IP:** `192.168.1.15`, chạy Server 1 trên cổng `1099`.
- **Máy 2 IP:** `192.168.1.25`, chạy Server 2 trên cổng `1099`.

1. **Lệnh chạy trên MÁY 1 (Server 1):**
   ```cmd
   java -cp bin com.vku.rmi.server.RmiSyncServer -id SERVER_1 -host 192.168.1.15 -port 1099 -partnerHost 192.168.1.25 -partnerPort 1099 -partnerId SERVER_2
   ```

2. **Lệnh chạy trên MÁY 2 (Server 2):**
   ```cmd
   java -cp bin com.vku.rmi.server.RmiSyncServer -id SERVER_2 -host 192.168.1.25 -port 1099 -partnerHost 192.168.1.15 -partnerPort 1099 -partnerId SERVER_1
   ```

3. **Lệnh chạy CLIENT (trên Máy 1, Máy 2 hoặc Máy Ảo bất kỳ):**
   ```cmd
   java -cp bin com.vku.rmi.client.RmiClientGUI 192.168.1.15 1099 192.168.1.25 1099
   ```

4. **Lệnh chạy BẢNG ĐIỀU KHIỂN MONITOR GIÁM SÁT CỤM:**
   ```cmd
   java -cp bin com.vku.rmi.monitor.ClusterMonitorGUI 192.168.1.15 1099 192.168.1.25 1099
   ```

---

## 4. Kịch Bản Kiểm Tra Cho Giảng Viên (Test Scenarios)

Dưới đây là quy trình chuẩn để biểu diễn trọn vẹn các yêu cầu của bài Lab trước Giảng viên:

### Kịch bản 1: Đồng bộ thời gian thực từ Máy 1 sang Máy 2
1. Trên giao diện Client (đang kết nối tới Server 1 trên Máy 1), nhập thông tin:
   - Mã SV: `24NS001`
   - Tên: `Nguyễn Văn An`
   - Khoa: `CNTT` | GPA: `3.80`
   - Bấm **➕ Thêm Mới Bản Ghi**.
2. **Quan sát kết quả:**
   - Màn hình Máy 1 log: `[RMI GỬI] Đồng bộ tạo mới sang SERVER_2: 24NS001`.
   - Màn hình Máy 2 log: `[RMI SYNC NHẬN] Tạo bản ghi mới từ SERVER_1: 24NS001`.
   - Trên bảng `Cluster Monitor`, bản ghi `24NS001` xuất hiện ngay lập tức với trạng thái `✓ ĐÃ ĐỒNG BỘ 100%`.

### Kịch bản 2: Đồng bộ 2 chiều từ Máy 2 ngược lại Máy 1 (Active-Active)
1. Mở một Client khác kết nối tới Server 2 (Máy 2) hoặc bấm nút **Chuyển Server** trên Client.
2. Thêm sinh viên: `24NS002 - Trần Thị Bình`.
3. **Quan sát kết quả:** Dữ liệu lập tức được đẩy ngược về Server 1 trên Máy 1 mà **không bị lặp vô tận (Replication Loop)**.

### Kịch bản 3: Kiểm tra tính chịu lỗi (Automatic Failover)
1. Giả lập sự cố mất điện / sập mạng: Đóng cửa sổ Server 1 trên Máy 1 (hoặc tắt Wi-Fi Máy 1).
2. Trên Client, thực hiện thao tác Thêm sinh viên `24NS003 - Lê Hoàng Long`.
3. **Quan sát:** Client lập tức phát hiện Server 1 mất tín hiệu, tự động Failover chuyển kết nối sang Server 2 trên Máy 2. Bản ghi `24NS003` được thêm thành công vào Server 2 mà Client không bị crash hay báo lỗi ngắt quãng.

### Kịch bản 4: Kiểm tra đồng bộ bù khi Server phục hồi (Catch-Up Sync)
1. Trong lúc Server 1 vẫn đang tắt, tiếp tục thêm sinh viên `24NS004 - Vũ Minh Tuấn` trên Server 2.
2. Lúc này Server 2 có 4 bản ghi, còn Server 1 mới chỉ có 2 bản ghi.
3. Bật lại Server 1 trên Máy 1 (`run_server_lan.bat`).
4. **Quan sát:** Ngay khi Server 1 khởi động, cơ chế Heartbeat bắt tay lại:
   - Server 1 log: `-> Bắt đầu quá trình đồng bộ bù dữ liệu (Catch-Up Sync)...`
   - Server 1 gọi RMI `syncAll()` kéo toàn bộ các bản ghi mới từ Server 2 về.
   - Kết quả: Cả 2 Server trên 2 máy thực tế đều đồng nhất 100% (cùng sở hữu 4 bản ghi, cùng phiên bản).

---

## 5. Hướng Dẫn Test Trên 1 Máy (Localhost)
Nếu cần kiểm tra nhanh tính năng trước khi mang lên lớp test 2 máy:
1. Chạy file: `run_cluster.bat`
2. Hệ thống tự động mở:
   - Server 1 (Port 1099)
   - Server 2 (Port 1100)
   - Bảng giám sát cụm (Cluster Monitor GUI)
   - Client GUI
3. Để thử nghiệm nhiều máy trạm: Chạy thêm `run_multi_clients.bat` (mở 4 Client cùng lúc).

---

## 6. Câu Hỏi Giáo Viên Thường Hỏi (Vấn Đáp & Lý Thuyết)

### Q1: Java RMI hoạt động như thế nào? Cần những thành phần cốt lõi nào?
- **Java RMI (Remote Method Invocation):** Cơ chế cho phép một đối tượng chạy trên máy ảo Java (JVM) này có thể triệu gọi trực tiếp phương thức của một đối tượng chạy trên một JVM khác (kể cả khác máy tính trong mạng LAN).
- **Các thành phần cốt lõi:**
  1. **Remote Interface (Giao diện từ xa):** Kế thừa `java.rmi.Remote`, các phương thức ném ngoại lệ `java.rmi.RemoteException`.
  2. **Remote Object Implementation:** Triển khai Remote Interface và kế thừa `UnicastRemoteObject`.
  3. **RMI Registry (Bộ đăng ký RMI):** Đóng vai trò là Name Service (dịch vụ danh bạ tên), thường chạy ở cổng 1099 để bind đối tượng với tên định danh và cho phép Client lookup tìm kiếm.
  4. **Stub & Skeleton:** Lớp trung gian thực hiện Marshal (đóng gói tham số nhị phân gửi qua mạng) và Unmarshal (giải nén dữ liệu kết quả trả về).

### Q2: Làm thế nào để tránh vòng lặp đồng bộ vô hạn (Replication Loop) giữa 2 Server?
- Khi Server 1 nhận lệnh từ Client, nó ghi nhận `originServerId = "SERVER_1"` và gọi hàm `syncCreate(record, "SERVER_1")` sang Server 2.
- Khi Server 2 nhận được hàm này, nó kiểm tra thấy `originServerId` là của đối tác chứ không phải do Client trực tiếp gửi tới Server 2, do đó Server 2 chỉ cập nhật vào database của mình mà **KHÔNG gọi ngược lại** `syncCreate` về Server 1. Điều này loại bỏ hoàn toàn tình trạng loop vô tận.

### Q3: Khi test 2 máy qua mạng LAN, tại sao cần thiết lập `java.rmi.server.hostname`?
- **Nguyên nhân:** Mặc định, Java RMI Stub có thể tự gán địa chỉ IP loopback `127.0.0.1` hoặc IP card mạng ảo. Khi một máy tính khác từ xa lấy Stub về và gọi hàm, nó sẽ cố kết nối vào `127.0.0.1` của chính nó và gây lỗi `Connection refused`.
- **Giải pháp:** Trong `RmiSyncServer.java`, hệ thống đã cấu hình:
  ```java
  System.setProperty("java.rmi.server.hostname", host);
  ```
  Điều này đảm bảo Stub gửi tới các máy khác luôn chứa đúng địa chỉ IP mạng LAN thực tế của Server.

---

## 7. Xử Lý Các Sự Cố Thường Gặp (Troubleshooting)

| Hiện Tượng Lỗi | Nguyên Nhân | Cách Khắc Phục |
| :--- | :--- | :--- |
| `java.rmi.ConnectException: Connection refused to host: 127.0.0.1` | Chưa cấu hình IP LAN khi khởi động Server hoặc Client đang trỏ vào 127.0.0.1. | Đảm bảo khi chạy trên 2 máy phải truyền đúng IP LAN (dùng `run_server_lan.bat`). |
| `Connection timed out` hoặc không kết nối được giữa 2 máy | 1. Tường lửa Windows chặn cổng RMI.<br>2. Hai máy không cùng chung dải mạng LAN. | 1. Tắt tạm thời Windows Firewall hoặc chạy lệnh mở cổng ở Bước 2.<br>2. Kiểm tra `ping` qua lại giữa 2 máy. |
| Wi-Fi trường đại học không cho ping thấy nhau | Tính năng AP Isolation trên Router của trường chặn giao tiếp giữa các thiết bị con. | Dùng 1 máy tính bật Windows Mobile Hotspot rồi cho máy kia kết nối vào. |
| `Address already in use: JVM_Bind` | Cổng 1099 hoặc 1100 đang bị chiếm dụng bởi một tiến trình Java khác chạy ngầm. | Mở Task Manager tắt các tiến trình `java.exe` đang chạy ngầm, hoặc đổi sang cổng khác (ví dụ: 1098). |
