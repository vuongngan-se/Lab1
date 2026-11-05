# BÁO CÁO VÀ HƯỚNG DẪN LAB 4: WEB SERVER & MINI BROWSER (HTTP/1.1)

## 1. Yêu Cầu Đề Bài & Mức Độ Hoàn Thiện
- **Giao thức HTTP chuẩn (HTTP/1.1):** Xây dựng cả **Web Server** và **Web Client (Mini Browser)**.
- **Dịch vụ của Máy Chủ (Server có nhiều hơn 1 dịch vụ):**
  1. **Trang chủ Web Portal (.html + .css):** Giao diện web hiện đại giới thiệu hệ thống và điều hướng.
  2. **Dịch vụ Tài Liệu PDF (.pdf):** Phục vụ tệp PDF với MIME type `application/pdf`, hỗ trợ xem trực tiếp hoặc tải về máy.
  3. **Dịch vụ Âm Thanh MP3 (.mp3):** Phục vụ luồng phát âm thanh với MIME type `audio/mpeg`.
  4. **Dịch vụ Video Streaming MP4 (.mp4):** Phục vụ video với hỗ trợ **Byte-Range requests (HTTP 206 Partial Content)** và `Accept-Ranges: bytes` cho phép tua nhanh video không cần tải toàn bộ tệp.
  5. **Dịch vụ REST API JSON (`/api/stats`, `/api/echo`):** Trả về dữ liệu thống kê máy chủ dạng JSON.
- **Trình Duyệt Web (Mini Browser) tương tự Chrome / Cốc Cốc:**
  - Thanh địa chỉ URL bar, các nút điều hướng: Quay lại (`←`), Tiến tới (`→`), Tải lại (`🔄`), Về trang chủ (`🏠`).
  - Hỗ trợ các phương thức HTTP:
    - **GET:** Tải trang HTML, hình ảnh, tài liệu hoặc gọi API.
    - **HEAD:** Kiểm tra mã trạng thái và toàn bộ Response Headers mà không tải phần thân body.
    - **POST:** Nhập dữ liệu biểu mẫu (Form Data) hoặc JSON ở tab "📮 Gửi Dữ Liệu HTTP POST" và nhận kết quả xử lý từ server.
  - **Lịch sử duyệt web (History):** Quản lý toàn bộ danh sách các URL đã ghé thăm kèm ngày giờ và mã phản hồi; nhấp đúp để duyệt lại trang cũ.
  - **Trình quản lý tải xuống (Download Manager):** Tải bất kỳ tệp tin nào (.pdf, .mp3, .mp4...) với thanh tiến trình tiến độ (0% - 100%), thông báo số byte đã tải, và nút mở thư mục lưu trữ.

---

## 2. Cách Khởi Động & Chấm Điểm
1. **Khởi động Web Server:** Chạy `run_server.bat`.
   - Bảng điều khiển Server sẽ mở ra, hiển thị trạng thái lắng nghe trên cổng 8080.
   - Thư mục `webroot/` được tự động khởi tạo với đầy đủ các file mẫu `index.html`, `style.css`, `sample.pdf`, `sample.mp3`, `sample.mp4`.
   - Có thể mở trình duyệt Google Chrome hoặc Edge thật truy cập: `http://localhost:8080/`.
2. **Khởi động Mini Browser:** Chạy `run_browser.bat`.
   - Trình duyệt Mini Browser tự động mở và tải trang chủ `http://localhost:8080/`.
   - **Thử nghiệm GET:** Bấm vào các nút đánh dấu dịch vụ (PDF, MP3, MP4, REST API).
   - **Thử nghiệm HEAD:** Chọn phương thức `HEAD` trên thanh điều hướng hoặc bấm nút "🔍 Kiểm Tra HEAD", chuyển sang tab "📋 Headers & Mã Phản Hồi" để xem các header HTTP được phân tích chi tiết.
   - **Thử nghiệm POST:** Chuyển sang tab "📮 Gửi Dữ Liệu HTTP POST", nhập nội dung cần gửi và bấm "🚀 Gửi Yêu Cầu POST".
   - **Thử nghiệm Download:** Nhập link tệp (vd: `http://localhost:8080/sample.pdf`) và bấm nút "📥 Tải Tệp Này (Download)". Cửa sổ Download Manager sẽ hiện ra với thanh tiến trình tải.
   - **Thử nghiệm History:** Bấm nút "🕒 Lịch Sử (History)" để xem lại các trang web vừa truy cập.

---

## 3. Câu Hỏi Giáo Viên Thường Hỏi
### Q1: Phương thức GET, HEAD và POST khác nhau như thế nào?
- **GET:** Yêu cầu máy chủ gửi lại nội dung tài nguyên được xác định bởi URI. Tham số truyền tải được gắn kèm trên URL (Query string).
- **HEAD:** Tương tự như GET, nhưng máy chủ chỉ phản hồi dòng trạng thái và các Header (Content-Type, Content-Length, Last-Modified...) mà KHÔNG gửi phần thân (Message Body). Thường dùng để kiểm tra tệp có tồn tại không, kích thước bao nhiêu trước khi tải, hoặc kiểm tra xem tài nguyên có bị sửa đổi không.
- **POST:** Gửi dữ liệu tới máy chủ trong phần thân (Request Body) để tạo mới hoặc xử lý dữ liệu (ví dụ: submit form đăng ký, tải tệp lên).

### Q2: Cơ chế Byte-Range (HTTP 206 Partial Content) hoạt động như thế nào khi xem video MP4?
- Khi trình duyệt phát video, để hỗ trợ tính năng tua (seeking) đến một vị trí bất kỳ (ví dụ giây thứ 30):
  - Client gửi header: `Range: bytes=1048576-2097151` (yêu cầu chỉ lấy đoạn byte từ 1MB đến 2MB).
  - Server phản hồi mã `HTTP/1.1 206 Partial Content`, kèm theo header `Content-Range: bytes 1048576-2097151/10485760` và gửi chính xác đoạn byte đó. Nhờ đó, trình duyệt không cần tải toàn bộ tệp 10MB mà vẫn xem được ngay đoạn mong muốn.
