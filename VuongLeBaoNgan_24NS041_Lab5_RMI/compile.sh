#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR" || exit 1
mkdir -p bin
echo "[VKU - LTM] Đang biên dịch Lab 5 (RMI 2 Server Data Synchronization)..."
javac -encoding UTF-8 -d bin $(find src -name "*.java")
if [ $? -eq 0 ]; then
    echo "[✓] Biên dịch Lab 5 thành công! Các file .class đã lưu vào thư mục 'bin'."
else
    echo "[✗] Biên dịch thất bại!"
fi

