#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1

if [ ! -f "bin/com/vku/rmi/client/RmiClientGUI.class" ]; then
    echo "[VKU - LTM] Đang biên dịch mã nguồn trước..."
    mkdir -p bin
    javac -encoding UTF-8 -cp ".:lib/*" -d bin $(find src -name "*.java")
fi

echo "[VKU - LTM] Đang khởi động 4 RMI Client đại diện cho các máy ảo..."
java -cp "bin:lib/*" com.vku.rmi.client.RmiClientGUI 127.0.0.1 1099 127.0.0.1 1100 &
sleep 1
java -cp "bin:lib/*" com.vku.rmi.client.RmiClientGUI 127.0.0.1 1100 127.0.0.1 1099 &
sleep 1
java -cp "bin:lib/*" com.vku.rmi.client.RmiClientGUI 127.0.0.1 1099 127.0.0.1 1100 &
sleep 1
java -cp "bin:lib/*" com.vku.rmi.client.RmiClientGUI 127.0.0.1 1100 127.0.0.1 1099 &

echo "[✓] Đã khởi động 4 Client thành công!"

