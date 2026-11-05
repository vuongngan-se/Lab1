#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1

if [ ! -f "bin/com/vku/rmi/server/RmiSyncServer.class" ]; then
    echo "[VKU - LTM] Đang biên dịch mã nguồn trước..."
    mkdir -p bin
    javac -encoding UTF-8 -d bin $(find src -name "*.java")
fi

echo "[VKU - LTM] Khởi động Server 1 (Port RMI: 1099, Partner: 1100)..."
java -cp bin com.vku.rmi.server.RmiSyncServer -id SERVER_1 -host 127.0.0.1 -port 1099 -partnerHost 127.0.0.1 -partnerPort 1100 -partnerId SERVER_2

