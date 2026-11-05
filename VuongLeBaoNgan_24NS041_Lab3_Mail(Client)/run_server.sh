#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1

if [ ! -f "bin/com/vku/mail/server/MailServerGUI.class" ]; then
    echo "[VKU - LTM] Đang biên dịch mã nguồn trước..."
    mkdir -p bin
    javac -encoding UTF-8 -d bin $(find src -name "*.java")
fi

echo "[VKU - LTM] Khởi động Mail Server (SMTP: 2525, POP3: 1100)..."
java -cp bin com.vku.mail.server.MailServerGUI

