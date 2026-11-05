@echo off
chcp 65001 > nul
title [VKU] Khoi Dong Server RMI - Mang LAN / 2 May Thuc Te

if not exist "bin\com\vku\rmi\server\RmiSyncServer.class" (
    echo [VKU - LTM] Dang bien dich ma nguon...
    call compile.bat
)

cls
echo ================================================================================
echo         [VKU - LTM] KHOI DONG RMI SERVER TREN 2 MAY THUC TE (MANG LAN)
echo ================================================================================
echo.
echo Lưu ý: Hãy đảm bảo đã bật MySQL trên XAMPP (Port 3306) để lưu trữ vào MySQL.
echo        (Nếu không bật MySQL, hệ thống sẽ tự động dùng File Cache dự phòng).
echo.
echo Cac dia chi IP hien tai cua may nay:
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4"') do (
    echo    -%%a
)
echo.
echo --------------------------------------------------------------------------------
echo CHON VAI TRO CUA MAY NAY:
echo   [1] SERVER 1 (May chu so 1)
echo   [2] SERVER 2 (May chu so 2)
echo --------------------------------------------------------------------------------
set /p ROLE_CHOICE="Nhap lua chon [1 hoac 2, mac dinh 1]: "
if "%ROLE_CHOICE%"=="" set ROLE_CHOICE=1

if "%ROLE_CHOICE%"=="2" (
    set S_ID=SERVER_2
    set P_ID=SERVER_1
    set DEF_PORT=1099
    set DEF_P_PORT=1099
) else (
    set S_ID=SERVER_1
    set P_ID=SERVER_2
    set DEF_PORT=1099
    set DEF_P_PORT=1099
)

echo.
set /p MY_IP="Nhap dia chi IPv4 cua may nay (VD: 192.168.1.10): "
if "%MY_IP%"=="" (
    echo [!] Ban chua nhap IP may nay. Vui long chay lai va nhap dung IP mang LAN.
    pause
    exit /b
)

set /p PARTNER_IP="Nhap dia chi IPv4 cua may doi tac (VD: 192.168.1.20): "
if "%PARTNER_IP%"=="" (
    echo [!] Ban chua nhap IP doi tac. Vui long chay lai va nhap dung IP mang LAN.
    pause
    exit /b
)

set /p MY_PORT="Nhap cong RMI may nay [Mac dinh %DEF_PORT%]: "
if "%MY_PORT%"=="" set MY_PORT=%DEF_PORT%

set /p PARTNER_PORT="Nhap cong RMI may doi tac [Mac dinh %DEF_P_PORT%]: "
if "%PARTNER_PORT%"=="" set PARTNER_PORT=%DEF_P_PORT%

echo.
echo ================================================================================
echo THONG TIN KHOI DONG:
echo   - May chu: %S_ID% tai %MY_IP%:%MY_PORT%
echo   - Doi tac : %P_ID% tai %PARTNER_IP%:%PARTNER_PORT%
echo ================================================================================
echo.
echo Dang khoi dong Server...
start "VKU RMI %S_ID% (%MY_IP%:%MY_PORT%)" java -cp "bin;lib/*" com.vku.rmi.server.RmiSyncServer -id %S_ID% -host %MY_IP% -port %MY_PORT% -partnerHost %PARTNER_IP% -partnerPort %PARTNER_PORT% -partnerId %P_ID%
echo [OK] Da bat cua so Server. Ban co the kiem tra log tren cua so moi mo.
pause
