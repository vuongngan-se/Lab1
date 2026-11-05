@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động Server 2 (Port RMI: 1100, Partner: 1099)...
if not exist "bin\com\vku\rmi\server\RmiSyncServer.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/rmi/model/*.java src/com/vku/rmi/service/*.java src/com/vku/rmi/db/*.java src/com/vku/rmi/server/*.java src/com/vku/rmi/client/*.java src/com/vku/rmi/monitor/*.java
)
start "VKU RMI Server 2 (Port 1100)" java -cp "bin;lib/*" com.vku.rmi.server.RmiSyncServer -id SERVER_2 -host 127.0.0.1 -port 1100 -partnerHost 127.0.0.1 -partnerPort 1099 -partnerId SERVER_1
