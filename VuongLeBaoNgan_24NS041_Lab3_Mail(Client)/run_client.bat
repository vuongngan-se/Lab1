@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động Mail Client (Tương tự Gmail)...
if not exist "bin\com\vku\mail\client\MailClientGUI.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src/com/vku/mail/model/*.java src/com/vku/mail/db/*.java src/com/vku/mail/server/*.java src/com/vku/mail/client/*.java
)
start "VKU Mail Client (Gmail Style)" java -cp bin com.vku.mail.client.MailClientGUI
