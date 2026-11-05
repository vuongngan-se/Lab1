@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang biên dịch Lab 3: Mail Server va Mail Client (SMTP + POP3)...
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src/com/vku/mail/model/*.java src/com/vku/mail/db/*.java src/com/vku/mail/server/*.java src/com/vku/mail/client/*.java
if %ERRORLEVEL% equ 0 (
    echo [✓] Biên dịch thành công! Các file .class đã lưu vào thư mục 'bin'.
) else (
    echo [✗] Biên dịch thất bại!
)
pause
