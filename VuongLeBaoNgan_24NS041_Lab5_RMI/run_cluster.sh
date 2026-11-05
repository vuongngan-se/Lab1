#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1

if [ ! -f "bin/com/vku/rmi/server/RmiSyncServer.class" ]; then
    echo "[VKU - LTM] Đang biên dịch mã nguồn trước..."
    mkdir -p bin
    javac -encoding UTF-8 -d bin $(find src -name "*.java")
fi

echo "========================================================"
echo "[VKU - LTM] KHỞI ĐỘNG CỤM RMI CLUSTER TRÊN LINUX"
echo "========================================================"

echo "1. Khởi động Server 1 (Cổng 1099)..."
java -cp bin com.vku.rmi.server.RmiSyncServer -id SERVER_1 -host 127.0.0.1 -port 1099 -partnerHost 127.0.0.1 -partnerPort 1100 -partnerId SERVER_2 &
sleep 2

echo "2. Khởi động Server 2 (Cổng 1100)..."
java -cp bin com.vku.rmi.server.RmiSyncServer -id SERVER_2 -host 127.0.0.1 -port 1100 -partnerHost 127.0.0.1 -partnerPort 1099 -partnerId SERVER_1 &
sleep 2

echo "3. Khởi động Bảng điều khiển Cluster Monitor..."
java -cp bin com.vku.rmi.monitor.ClusterMonitorGUI 127.0.0.1 1099 127.0.0.1 1100 &
sleep 1

echo "4. Khởi động RMI Client..."
java -cp bin com.vku.rmi.client.RmiClientGUI 127.0.0.1 1099 127.0.0.1 1100 &

echo "[✓] Cụm đồng bộ RMI đã được khởi động!"

