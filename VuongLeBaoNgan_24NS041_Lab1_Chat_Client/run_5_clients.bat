@echo off
chcp 65001 > nul
echo ========================================================
echo [VKU - LTM] KHỞI ĐỘNG ĐỒNG THỜI 5-6 CLIENTS ĐỂ CHẤM ĐIỂM
echo ========================================================
echo 1. Alice  (alice / 123456)
echo 2. Bob    (bob / 123456)
echo 3. Charlie(charlie / 123456)
echo 4. David  (david / 123456)
echo 5. Eve    (eve / 123456)
echo 6. Frank  (frank / 123456)
echo.

set SERVER_HOST=localhost
if not "%~1"=="" set SERVER_HOST=%~1
echo Kết nối tới máy chủ: %SERVER_HOST%
echo.

if not exist "bin\comku\chat\client\ChatClientGUI.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/chat/model/*.java src/com/vku/chat/db/*.java src/com/vku/chat/server/*.java src/com/vku/chat/client/*.java
)

echo Đang bật Client 1 (Alice)...
start "Client 1 - Alice" java -cp bin com.vku.chat.client.ChatClientGUI alice 123456 %SERVER_HOST%
timeout /t 1 > nul

echo Đang bật Client 2 (Bob)...
start "Client 2 - Bob" java -cp bin com.vku.chat.client.ChatClientGUI bob 123456 %SERVER_HOST%
timeout /t 1 > nul

echo Đang bật Client 3 (Charlie)...
start "Client 3 - Charlie" java -cp bin com.vku.chat.client.ChatClientGUI charlie 123456 %SERVER_HOST%
timeout /t 1 > nul

echo Đang bật Client 4 (David)...
start "Client 4 - David" java -cp bin com.vku.chat.client.ChatClientGUI david 123456 %SERVER_HOST%
timeout /t 1 > nul

echo Đang bật Client 5 (Eve)...
start "Client 5 - Eve" java -cp bin com.vku.chat.client.ChatClientGUI eve 123456 %SERVER_HOST%
timeout /t 1 > nul

echo Đang bật Client 6 (Frank)...
start "Client 6 - Frank" java -cp bin com.vku.chat.client.ChatClientGUI frank 123456 %SERVER_HOST%

echo.
echo [✓] Đã khởi chạy xong 6 Clients!
