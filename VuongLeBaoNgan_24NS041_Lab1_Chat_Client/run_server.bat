@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động Chat Server (TCP 8888, UDP Multicast 8889)...
if not exist "bin\com\vku\chat\server\ChatServer.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/chat/model/*.java src/com/vku/chat/db/*.java src/com/vku/chat/server/*.java src/com/vku/chat/client/*.java
)
start "VKU Chat Server" java -cp bin com.vku.chat.server.ChatServer
