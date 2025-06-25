@echo off
REM setup_chrome.bat - Download Chrome per Windows

title Setup Chrome per Email Converter

echo.
echo 🚀 Chrome Setup per Email Converter
echo ===================================
echo 🪟 Windows rilevato

REM 1. Crea struttura directory
echo 📂 Verificando/creando struttura directory...

if not exist "lib" (
    mkdir "lib"
    echo ✅ Creata directory: lib\
) else (
    echo ✅ Directory esistente: lib\
)

if not exist "lib\windows" (
    mkdir "lib\windows"
    echo ✅ Creata directory: lib\windows\
) else (
    echo ✅ Directory esistente: lib\windows\
)

REM Verifica creazione riuscita
if not exist "lib\windows" (
    echo ❌ Errore: impossibile creare lib\windows\
    echo    Verifica i permessi e riprova
    pause
    exit /b 1
)

REM 2. Controlla se Chrome è già presente
set TARGET_DIR=lib\windows
set CHROME_EXE=%TARGET_DIR%\chrome.exe

if exist "%CHROME_EXE%" (
    echo.
    echo ✅ Chrome già presente in %TARGET_DIR%\

    REM Test veloce di Chrome
    echo 🧪 Test Chrome esistente...
    "%CHROME_EXE%" --version >nul 2>&1
    if %ERRORLEVEL%==0 (
        echo ✅ Chrome funziona correttamente
        echo 🎉 Setup già completato!
        echo.
        echo Per reinstallare: elimina la cartella %TARGET_DIR%\ e rilancia lo script
        pause
        exit /b 0
    ) else (
        echo ⚠️  Chrome presente ma non funziona, reinstallo...
    )
)

REM 3. Download Chrome
echo.
echo ⬇️  Scaricando Chrome per Windows...
set CHROME_URL=https://storage.googleapis.com/chrome-for-testing-public/stable/win64/chrome-win64.zip

echo    URL: %CHROME_URL%
echo    Dimensione: ~150 MB
echo    Destinazione: %TARGET_DIR%\

REM Download con PowerShell (disponibile su Windows 7+)
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Write-Host 'Download in corso, attendere...'; try { Invoke-WebRequest -Uri '%CHROME_URL%' -OutFile 'chrome.zip' -UseBasicParsing; Write-Host 'Download completato' } catch { Write-Host 'Errore download:' $_.Exception.Message; exit 1 }}"

if %ERRORLEVEL% neq 0 (
    echo ❌ Errore nel download di Chrome
    echo    Possibili cause:
    echo    - Connessione internet assente
    echo    - Firewall/antivirus blocca il download
    echo    - URL temporaneamente non disponibile
    if exist chrome.zip del chrome.zip
    pause
    exit /b 1
)

if not exist chrome.zip (
    echo ❌ File chrome.zip non trovato dopo il download
    pause
    exit /b 1
)

REM 4. Estrazione Chrome
echo.
echo 📦 Estraendo Chrome...

REM Estrazione con PowerShell
powershell -Command "try { Expand-Archive -Path 'chrome.zip' -DestinationPath '.' -Force; Write-Host 'Estrazione completata' } catch { Write-Host 'Errore estrazione:' $_.Exception.Message; exit 1 }"

if %ERRORLEVEL% neq 0 (
    echo ❌ Errore nell'estrazione
    if exist chrome.zip del chrome.zip
    pause
    exit /b 1
)

REM Sposta file dalla cartella estratta alla directory target
if exist "chrome-win64" (
    echo 📁 Spostando file Chrome in %TARGET_DIR%\...
    xcopy "chrome-win64\*" "%TARGET_DIR%\" /E /I /Y >nul 2>&1
    if %ERRORLEVEL%==0 (
        rmdir "chrome-win64" /S /Q >nul 2>&1
        echo ✅ File spostati con successo
    ) else (
        echo ❌ Errore nello spostamento file
        pause
        exit /b 1
    )
) else (
    echo ❌ Cartella chrome-win64 non trovata dopo estrazione
    pause
    exit /b 1
)

REM Pulisci file zip
del chrome.zip >nul 2>&1

REM 5. Verifica installazione finale
echo.
echo 🔍 Verifica installazione...

if exist "%CHROME_EXE%" (
    echo ✅ Chrome installato: %CHROME_EXE%

    REM Test finale
    echo 🧪 Test funzionalità Chrome...
    "%CHROME_EXE%" --version >nul 2>&1
    if %ERRORLEVEL%==0 (
        for /f "tokens=*" %%i in ('"%CHROME_EXE%" --version 2^>nul') do (
            echo ✅ Chrome funziona: %%i
        )
        echo.
        echo 🎉 Setup completato con successo!
        echo 📍 Chrome installato in: %CD%\%TARGET_DIR%\
        echo 🚀 Il converter è pronto per l'uso!
    ) else (
        echo ⚠️  Chrome installato ma test fallito
        echo    ^(Normale su alcuni sistemi, dovrebbe funzionare nell'applicazione^)
        echo.
        echo 🎉 Setup completato!
    )
else (
    echo ❌ Chrome non trovato dopo l'installazione
    echo    Qualcosa è andato storto durante l'estrazione
    pause
    exit /b 1
)

echo.
echo 📋 Prossimi passi:
echo    1. Compila il progetto Java
echo    2. Esegui l'applicazione
echo    3. Chrome verrà rilevato automaticamente
echo.
pause