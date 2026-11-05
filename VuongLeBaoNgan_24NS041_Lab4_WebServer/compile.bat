@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang biên dịch Lab 4: Web Server & Mini Browser (HTTP/1.1)...
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src/com/vku/web/common/*.java src/com/vku/web/server/*.java src/com/vku/web/browser/*.java
if %ERRORLEVEL% equ 0 (
    echo [✓] Biên dịch thành công! Các file .class đã lưu vào thư mục 'bin'.
) else (
    echo [✗] Biên dịch thất bại!
)
pause
