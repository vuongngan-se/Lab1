@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang khởi động Mail Server (SMTP: 2525, POP3: 1100 + MySQL Support)...
if not exist "bin\com\vku\mail\server\MailServerGUI.class" (
    echo Đang biên dịch mã nguồn trước...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -cp ".;lib/*" -d bin src/com/vku/mail/model/*.java src/com/vku/mail/db/*.java src/com/vku/mail/server/*.java src/com/vku/mail/client/*.java
)
start "VKU Mail Server Manager" java -cp "bin;lib/*" com.vku.mail.server.MailServerGUI
