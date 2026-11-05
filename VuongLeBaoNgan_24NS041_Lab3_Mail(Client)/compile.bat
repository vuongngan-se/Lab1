@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang biên dịch Lab 3: Mail Server ^& Mail Client (SMTP + POP3 + MySQL)...
if not exist "bin" mkdir "bin"
javac -encoding UTF-8 -cp ".;lib/*" -d bin src/com/vku/mail/model/*.java src/com/vku/mail/db/*.java src/com/vku/mail/server/*.java src/com/vku/mail/client/*.java
if %ERRORLEVEL% equ 0 (
    echo [OK] Biên dịch thành công! Thư mục bin đã được cập nhật.
) else (
    echo [FAIL] Biên dịch thất bại! Vui lòng kiểm tra lại lỗi trên.
)
pause
