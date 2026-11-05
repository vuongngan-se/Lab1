@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động FTP Client (Tương tự FileZilla)...
if not exist "bin\com\vku\ftp\client\FtpClientGUI.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/ftp/common/*.java src/com/vku/ftp/db/*.java src/com/vku/ftp/server/*.java src/com/vku/ftp/client/*.java
)
start "VKU FTP Client" java -cp bin com.vku.ftp.client.FtpClientGUI
