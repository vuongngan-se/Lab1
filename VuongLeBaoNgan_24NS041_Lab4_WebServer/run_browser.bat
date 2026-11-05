@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động Mini Browser (Tương tự Chrome/Cốc Cốc)...
if not exist "bin\com\vku\web\browser\MiniBrowserGUI.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/web/common/*.java src/com/vku/web/server/*.java src/com/vku/web/browser/*.java
)
start "VKU Mini Browser" java -cp bin com.vku.web.browser.MiniBrowserGUI
