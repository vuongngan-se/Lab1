@echo off
chcp 65001 > nul
echo ===================================================================
echo [VKU - LTM] BIEN DICH TOAN BO 5 BAI LAB
echo ===================================================================

echo.
echo [1/5] Dang bien dich Lab 1: Chat Application...
cd /d "%~dp0lab1"
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src/com/vku/chat/model/*.java src/com/vku/chat/db/*.java src/com/vku/chat/server/*.java src/com/vku/chat/client/*.java
if %ERRORLEVEL% equ 0 (
    echo     - Lab 1: THANH CONG
) else (
    echo     - Lab 1: THAT BAI
)

echo.
echo [2/5] Dang bien dich Lab 2: FTP Server va FTP Client...
cd /d "%~dp0lab2"
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src/com/vku/ftp/common/*.java src/com/vku/ftp/db/*.java src/com/vku/ftp/server/*.java src/com/vku/ftp/client/*.java
if %ERRORLEVEL% equ 0 (
    echo     - Lab 2: THANH CONG
) else (
    echo     - Lab 2: THAT BAI
)

echo.
echo [3/5] Dang bien dich Lab 3: Mail Server va Mail Client...
cd /d "%~dp0lab3"
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src/com/vku/mail/model/*.java src/com/vku/mail/db/*.java src/com/vku/mail/server/*.java src/com/vku/mail/client/*.java
if %ERRORLEVEL% equ 0 (
    echo     - Lab 3: THANH CONG
) else (
    echo     - Lab 3: THAT BAI
)

echo.
echo [4/5] Dang bien dich Lab 4: Web Server va Mini Browser...
cd /d "%~dp0lab4"
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src/com/vku/web/common/*.java src/com/vku/web/server/*.java src/com/vku/web/browser/*.java
if %ERRORLEVEL% equ 0 (
    echo     - Lab 4: THANH CONG
) else (
    echo     - Lab 4: THAT BAI
)

echo.
echo [5/5] Dang bien dich Lab 5: RMI Dong Bo Hoa 2 Server...
cd /d "%~dp0lab5"
if not exist "bin" mkdir bin
javac -encoding UTF-8 -d bin src/com/vku/rmi/model/*.java src/com/vku/rmi/service/*.java src/com/vku/rmi/db/*.java src/com/vku/rmi/server/*.java src/com/vku/rmi/client/*.java src/com/vku/rmi/monitor/*.java
if %ERRORLEVEL% equ 0 (
    echo     - Lab 5: THANH CONG
) else (
    echo     - Lab 5: THAT BAI
)

cd /d "%~dp0"
echo.
echo ===================================================================
echo [OK] DA HOAN TAT BIEN DICH TOAN BO 5 BAI LAB!
echo ===================================================================
pause
