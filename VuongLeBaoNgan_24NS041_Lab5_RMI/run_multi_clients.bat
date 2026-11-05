@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động 4 Client mô phỏng nhiều máy ảo truy cập cụm RMI...
if not exist "bin\com\vku\rmi\client\RmiClientGUI.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/rmi/model/*.java src/com/vku/rmi/service/*.java src/com/vku/rmi/db/*.java src/com/vku/rmi/server/*.java src/com/vku/rmi/client/*.java src/com/vku/rmi/monitor/*.java
)

start "RMI Client 1 (VM-1)" java -cp "bin;lib/*" com.vku.rmi.client.RmiClientGUI 127.0.0.1 1099 127.0.0.1 1100
timeout /t 1 > nul
start "RMI Client 2 (VM-2)" java -cp "bin;lib/*" com.vku.rmi.client.RmiClientGUI 127.0.0.1 1100 127.0.0.1 1099
timeout /t 1 > nul
start "RMI Client 3 (VM-3)" java -cp "bin;lib/*" com.vku.rmi.client.RmiClientGUI 127.0.0.1 1099 127.0.0.1 1100
timeout /t 1 > nul
start "RMI Client 4 (VM-4)" java -cp "bin;lib/*" com.vku.rmi.client.RmiClientGUI 127.0.0.1 1100 127.0.0.1 1099

echo [✓] Đã khởi động 4 Client thành công!
pause
