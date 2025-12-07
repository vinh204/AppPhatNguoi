@echo off
echo ========================================
echo Fix Burp Suite Proxy - No Internet
echo ========================================
echo.

REM Check if ADB is available
where adb >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] ADB not found! Please add Android SDK platform-tools to PATH.
    pause
    exit /b 1
)

REM Check if device is connected
echo [1/6] Checking device connection...
adb devices | findstr "device$" >nul
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] No Android device/emulator connected!
    echo Please connect device or start emulator first.
    pause
    exit /b 1
)
echo [OK] Device connected.

REM Ask user: Emulator or Physical Device
echo.
echo Are you using:
echo 1. Android Emulator
echo 2. Physical Device (via Wi-Fi)
set /p choice="Enter choice (1 or 2): "

if "%choice%"=="1" (
    set PROXY_HOST=10.0.2.2
    echo [INFO] Using emulator proxy: 10.0.2.2:8080
) else if "%choice%"=="2" (
    echo.
    echo Please enter your computer's IP address (from ipconfig)
    echo Example: 192.168.133.104
    set /p PROXY_HOST="Enter IP: "
    if "%PROXY_HOST%"=="" (
        echo [ERROR] IP address cannot be empty!
        pause
        exit /b 1
    )
    echo [INFO] Using device proxy: %PROXY_HOST%:8080
) else (
    echo [ERROR] Invalid choice!
    pause
    exit /b 1
)

echo.
echo ========================================
echo Fixing Proxy Settings
echo ========================================

REM Step 2: Remove old proxy settings
echo.
echo [2/6] Removing old proxy settings...
adb shell settings delete global http_proxy >nul 2>nul
adb shell settings delete global global_http_proxy_host >nul 2>nul
adb shell settings delete global global_http_proxy_port >nul 2>nul
echo [OK] Old proxy settings removed.

REM Step 3: Set new proxy
echo.
echo [3/6] Setting new proxy: %PROXY_HOST%:8080...
adb shell settings put global http_proxy %PROXY_HOST%:8080
if %ERRORLEVEL% EQU 0 (
    echo [OK] Proxy set successfully.
) else (
    echo [ERROR] Failed to set proxy!
    pause
    exit /b 1
)

REM Step 4: Verify proxy
echo.
echo [4/6] Verifying proxy settings...
for /f "tokens=*" %%a in ('adb shell settings get global http_proxy') do set PROXY_RESULT=%%a
echo Current proxy: %PROXY_RESULT%
if "%PROXY_RESULT%"=="%PROXY_HOST%:8080" (
    echo [OK] Proxy verified correctly.
) else (
    echo [WARNING] Proxy may not be set correctly.
)

REM Step 5: Check Burp Suite connectivity
echo.
echo [5/6] Testing connectivity to proxy...
if "%choice%"=="1" (
    echo Testing connection to 10.0.2.2:8080...
    adb shell ping -c 1 10.0.2.2 >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        echo [OK] Can reach proxy host.
    ) else (
        echo [WARNING] Cannot ping proxy host. Check if Burp Suite is running.
    )
)

REM Step 6: Instructions
echo.
echo ========================================
echo Next Steps
echo ========================================
echo.
echo [6/6] IMPORTANT: Check Burp Suite:
echo   1. Open Burp Suite
echo   2. Go to Proxy ^> Intercept
echo   3. Make sure "Intercept is off" (toggle it off if needed)
echo   4. Go to Proxy ^> Options ^> Proxy Listeners
echo   5. Ensure listener is running on *:8080 or 0.0.0.0:8080
echo.
echo Then test:
echo   1. Open browser in device/emulator
echo   2. Visit: http://burpsuite or http://127.0.0.1:8080
echo   3. You should see Burp Suite page
echo.
echo If still no Internet:
echo   - Check Burp Suite is running
echo   - Check firewall is not blocking port 8080
echo   - Install Burp Suite CA certificate (see BURP_FIX_NO_INTERNET.md)
echo.
echo ========================================
echo Done!
echo ========================================
pause