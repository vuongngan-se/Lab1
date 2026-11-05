#!/bin/bash
# ===================================================================
# [VKU - LTM] BIÊN DỊCH TOÀN BỘ 5 BÀI LAB TRÊN LINUX / MACOS
# Sinh viên: Vương Lê Bảo Ngân - MSSV: 24NS041
# ===================================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==================================================================="
echo "[VKU - LTM] BẮT ĐẦU BIÊN DỊCH TOÀN BỘ 5 BÀI LAB TRÊN LINUX"
echo "Sinh viên: Vương Lê Bảo Ngân - MSSV: 24NS041"
echo "==================================================================="

echo ""
echo "[1/5] Biên dịch Lab 1: Chat Application..."
cd "$DIR/VuongLeBaoNgan_24NS041_Lab1_Chat_Client" || exit 1
mkdir -p bin
javac -encoding UTF-8 -d bin $(find src -name "*.java") && echo "    -> Lab 1: THÀNH CÔNG" || echo "    -> Lab 1: THẤT BẠI"

echo ""
echo "[2/5] Biên dịch Lab 2: FTP Server & Client..."
cd "$DIR/VuongLeBaoNgan_24NS041_Lab2_FTP" || exit 1
mkdir -p bin
javac -encoding UTF-8 -cp ".:lib/*" -d bin $(find src -name "*.java") && echo "    -> Lab 2: THÀNH CÔNG" || echo "    -> Lab 2: THẤT BẠI"

echo ""
echo "[3/5] Biên dịch Lab 3: Mail Server & Client..."
cd "$DIR/VuongLeBaoNgan_24NS041_Lab3_Mail(Client)" || exit 1
mkdir -p bin
javac -encoding UTF-8 -cp ".:lib/*" -d bin $(find src -name "*.java") && echo "    -> Lab 3: THÀNH CÔNG" || echo "    -> Lab 3: THẤT BẠI"

echo ""
echo "[4/5] Biên dịch Lab 4: Web Server & Mini Browser..."
cd "$DIR/VuongLeBaoNgan_24NS041_Lab4_WebServer" || exit 1
mkdir -p bin
javac -encoding UTF-8 -cp ".:lib/*" -d bin $(find src -name "*.java") && echo "    -> Lab 4: THÀNH CÔNG" || echo "    -> Lab 4: THẤT BẠI"

echo ""
echo "[5/5] Biên dịch Lab 5: RMI Đa Server..."
cd "$DIR/VuongLeBaoNgan_24NS041_Lab5_RMI" || exit 1
mkdir -p bin
javac -encoding UTF-8 -cp ".:lib/*" -d bin $(find src -name "*.java") && echo "    -> Lab 5: THÀNH CÔNG" || echo "    -> Lab 5: THẤT BẠI"

cd "$DIR" || exit 1
echo ""
echo "==================================================================="
echo "[✓] HOÀN TẤT BIÊN DỊCH 5 BÀI LAB TRÊN LINUX!"
echo "==================================================================="
