@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động RMI Client...
if not exist "bin\com\vku\rmi\client\RmiClientGUI.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/rmi/model/*.java src/com/vku/rmi/service/*.java src/com/vku/rmi/db/*.java src/com/vku/rmi/server/*.java src/com/vku/rmi/client/*.java src/com/vku/rmi/monitor/*.java
)
start "VKU RMI Client" java -cp "bin;lib/*" com.vku.rmi.client.RmiClientGUI %*
