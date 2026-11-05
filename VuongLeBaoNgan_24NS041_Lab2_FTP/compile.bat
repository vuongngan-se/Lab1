@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang biên dịch Lab 2: FTP Server ^& FTP Client (RFC 959 + MySQL)...
if not exist "bin" mkdir "bin"
javac -encoding UTF-8 -cp ".;lib/*" -d bin src/com/vku/ftp/common/*.java src/com/vku/ftp/db/*.java src/com/vku/ftp/server/*.java src/com/vku/ftp/client/*.java
if %ERRORLEVEL% equ 0 (
    echo [OK] Biên dịch thành công! Thư mục bin đã được cập nhật.
) else (
    echo [FAIL] Biên dịch thất bại! Vui lòng kiểm tra lại lỗi trên.
)
pause
