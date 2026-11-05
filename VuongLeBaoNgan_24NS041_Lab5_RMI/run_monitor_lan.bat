@echo off
chcp 65001 > nul
title [VKU] Khoi Dong Cluster Monitor - Theo Doi 2 Server Mang LAN

if not exist "bin\com\vku\rmi\monitor\ClusterMonitorGUI.class" (
    echo [VKU - LTM] Dang bien dich ma nguon...
    call compile.bat
)

cls
echo ================================================================================
echo     [VKU - LTM] KHOI DONG CLUSTER MONITOR GIAM SAT 2 SERVER (MANG LAN)
echo ================================================================================
echo.
set /p S1_IP="Nhap IP Server 1 (VD: 192.168.1.10): "
if "%S1_IP%"=="" (
    echo [!] Chua nhap IP Server 1. Mac dinh su dung 127.0.0.1
    set S1_IP=127.0.0.1
)

set /p S1_PORT="Nhap Port Server 1 [Mac dinh 1099]: "
if "%S1_PORT%"=="" set S1_PORT=1099

set /p S2_IP="Nhap IP Server 2 (VD: 192.168.1.20): "
if "%S2_IP%"=="" (
    echo [!] Chua nhap IP Server 2. Mac dinh su dung 127.0.0.1
    set S2_IP=127.0.0.1
)

set /p S2_PORT="Nhap Port Server 2 [Mac dinh 1099]: "
if "%S2_PORT%"=="" set S2_PORT=1099

echo.
echo Dang khoi dong Cluster Monitor giam sat:
echo   - Server 1: %S1_IP%:%S1_PORT%
echo   - Server 2: %S2_IP%:%S2_PORT%
echo.
start "VKU RMI Cluster Monitor" java -cp "bin;lib/*" com.vku.rmi.monitor.ClusterMonitorGUI %S1_IP% %S1_PORT% %S2_IP% %S2_PORT%
