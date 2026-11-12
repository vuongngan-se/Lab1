#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1

if [ ! -f "bin/com/vku/ftp/client/FtpClientGUI.class" ]; then
    echo "[VKU - LTM] Đang biên dịch mã nguồn trước..."
    mkdir -p bin
    javac -encoding UTF-8 -cp ".:lib/*" -d bin $(find src -name "*.java")
fi

echo "[VKU - LTM] Khởi động FTP Client (FileZilla Style)..."
java -cp "bin:lib/*" com.vku.ftp.client.FtpClientGUI

