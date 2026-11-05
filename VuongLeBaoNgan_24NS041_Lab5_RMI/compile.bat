@echo off
chcp 65001 > nul
echo [VKU - LTM] Đang biên dịch Lab 5: RMI Đồng Bộ Hóa Dữ Liệu 2 Server...
if not exist "bin" mkdir bin
javac -encoding UTF-8 -cp "lib/*" -d bin src/com/vku/rmi/model/*.java src/com/vku/rmi/service/*.java src/com/vku/rmi/db/*.java src/com/vku/rmi/server/*.java src/com/vku/rmi/client/*.java src/com/vku/rmi/monitor/*.java
if %ERRORLEVEL% equ 0 goto :ok
echo [!] Bien dich that bai!
goto :done
:ok
echo [OK] Bien dich thanh cong! Cac file .class da duoc luu vao thu muc 'bin'.
:done
pause
