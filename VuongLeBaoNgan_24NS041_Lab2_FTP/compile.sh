#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1
mkdir -p bin
echo "[VKU - LTM] Đang biên dịch Lab 2 (FTP Server & Client + MySQL)..."
javac -encoding UTF-8 -cp ".:lib/*" -d bin $(find src -name "*.java")
if [ $? -eq 0 ]; then
    echo "[✓] Biên dịch Lab 2 thành công! Các file .class đã lưu vào thư mục 'bin'."
else
    echo "[✗] Biên dịch thất bại!"
fi
