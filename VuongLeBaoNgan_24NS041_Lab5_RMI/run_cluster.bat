@echo off
chcp 65001 > nul
echo ========================================================
echo [VKU - LTM] KHỞI ĐỘNG CỤM RMI CLUSTER (2 SERVERS + MONITOR + CLIENT)
echo ========================================================

if not exist "bin\com\vku\rmi\server\RmiSyncServer.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -cp "lib/*" -d bin src/com/vku/rmi/model/*.java src/com/vku/rmi/service/*.java src/com/vku/rmi/db/*.java src/com/vku/rmi/server/*.java src/com/vku/rmi/client/*.java src/com/vku/rmi/monitor/*.java
)

echo 1. Khởi động Server 1 (Cổng 1099)...
start "VKU RMI Server 1 (Port 1099)" java -cp "bin;lib/*" com.vku.rmi.server.RmiSyncServer -id SERVER_1 -host 127.0.0.1 -port 1099 -partnerHost 127.0.0.1 -partnerPort 1100 -partnerId SERVER_2
timeout /t 2 > nul

echo 2. Khởi động Server 2 (Cổng 1100)...
start "VKU RMI Server 2 (Port 1100)" java -cp "bin;lib/*" com.vku.rmi.server.RmiSyncServer -id SERVER_2 -host 127.0.0.1 -port 1100 -partnerHost 127.0.0.1 -partnerPort 1099 -partnerId SERVER_1
timeout /t 2 > nul

echo 3. Khởi động Bảng Điều Khiển Giám Sát Cụm (Cluster Monitor GUI)...
start "VKU RMI Cluster Monitor" java -cp "bin;lib/*" com.vku.rmi.monitor.ClusterMonitorGUI 127.0.0.1 1099 127.0.0.1 1100
timeout /t 1 > nul

echo 4. Khởi động RMI Client...
start "VKU RMI Client" java -cp "bin;lib/*" com.vku.rmi.client.RmiClientGUI 127.0.0.1 1099 127.0.0.1 1100

echo [✓] Cụm đồng bộ RMI đã hoạt động!
pause
