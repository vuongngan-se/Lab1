@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động Web Server (Port 8080)...
if not exist "bin\com\vku\web\server\WebServerGUI.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -cp ".;lib/*" -d bin src/com/vku/web/common/*.java src/com/vku/web/db/*.java src/com/vku/web/server/*.java src/com/vku/web/browser/*.java
)
start "VKU Web Server (HTTP/1.1)" java -cp "bin;lib/*" com.vku.web.server.WebServerGUI
