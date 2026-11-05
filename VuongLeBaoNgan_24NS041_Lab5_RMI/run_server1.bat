@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động Server 1 (Port RMI: 1099, Partner: 1100)...
if not exist "bin\com\vku\rmi\server\RmiSyncServer.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/rmi/model/*.java src/com/vku/rmi/service/*.java src/com/vku/rmi/db/*.java src/com/vku/rmi/server/*.java src/com/vku/rmi/client/*.java src/com/vku/rmi/monitor/*.java
)
start "VKU RMI Server 1 (Port 1099)" java -cp "bin;lib/*" com.vku.rmi.server.RmiSyncServer -id SERVER_1 -host 127.0.0.1 -port 1099 -partnerHost 127.0.0.1 -partnerPort 1100 -partnerId SERVER_2
