@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động Chat Client...
if not exist "bin\com\vku\chat\client\ChatClientGUI.class" (
    echo Đang biên dịch mã nguồn...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/chat/model/*.java src/com/vku/chat/db/*.java src/com/vku/chat/server/*.java src/com/vku/chat/client/*.java
)
start "VKU Chat Client" java -cp bin com.vku.chat.client.ChatClientGUI %*
