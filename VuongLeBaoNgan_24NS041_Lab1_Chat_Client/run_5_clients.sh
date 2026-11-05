#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1

if [ ! -f "bin/com/vku/chat/client/ChatClientGUI.class" ]; then
    echo "[VKU - LTM] Đang biên dịch mã nguồn trước..."
    mkdir -p bin
    javac -encoding UTF-8 -d bin $(find src -name "*.java")
fi

echo "========================================================"
echo "[VKU - LTM] KHỞI ĐỘNG ĐỒNG THỜI 6 CLIENTS TRÊN LINUX"
echo "========================================================"
echo "1. Alice   (alice / 123456)"
echo "2. Bob     (bob / 123456)"
echo "3. Charlie (charlie / 123456)"
echo "4. David   (david / 123456)"
echo "5. Eve     (eve / 123456)"
echo "6. Frank   (frank / 123456)"
echo ""

java -cp bin com.vku.chat.client.ChatClientGUI alice 123456 &
sleep 1
java -cp bin com.vku.chat.client.ChatClientGUI bob 123456 &
sleep 1
java -cp bin com.vku.chat.client.ChatClientGUI charlie 123456 &
sleep 1
java -cp bin com.vku.chat.client.ChatClientGUI david 123456 &
sleep 1
java -cp bin com.vku.chat.client.ChatClientGUI eve 123456 &
sleep 1
java -cp bin com.vku.chat.client.ChatClientGUI frank 123456 &

echo "[✓] Đã khởi động 6 clients chạy ngầm thành công!"

