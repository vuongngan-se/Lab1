@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang biên dịch Lab 1: Chat Application...
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src/com/vku/chat/model/*.java src/com/vku/chat/db/*.java src/com/vku/chat/server/*.java src/com/vku/chat/client/*.java
if %ERRORLEVEL% equ 0 (
    echo [✓] Biên dịch thành công! Các file .class đã lưu vào thư mục 'bin'.
) else (
    echo [✗] Biên dịch thất bại! Vui lòng kiểm tra lỗi bên trên.
)
pause
