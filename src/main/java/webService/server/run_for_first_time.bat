@echo off
REM Download Chrome Headless Shell per Windows

title Setup Chrome Headless Shell per Email Converter

echo.
echo 🚀 Chrome Headless Shell Setup per Email Converter
echo ==========================================================
echo 🪟 Windows rilevato

REM 1. Rileva architettura Windows
echo 🔍 Rilevamento architettura...

REM Verifica se è un sistema 64-bit
if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
    set ARCH=x64
    set TARGET_DIR=lib\windows
    set CHROME_URL=https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/win64/chrome-headless-shell-win64.zip
    set EXTRACT_DIR=chrome-headless-shell-win64
    echo ✅ Architettura: 64-bit (x64)
) else if "%PROCESSOR_ARCHITEW6432%"=="AMD64" (
    set ARCH=x64
    set TARGET_DIR=lib\windows
    set CHROME_URL=https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/win64/chrome-headless-shell-win64.zip
    set EXTRACT_DIR=chrome-headless-shell-win64
    echo ✅ Architettura: 64-bit (x64) - processo WOW64
) else (
    set ARCH=x86
    set TARGET_DIR=lib\windows
    set CHROME_URL=https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/win32/chrome-headless-shell-win32.zip
    set EXTRACT_DIR=chrome-headless-shell-win32
    echo ✅ Architettura: 32-bit (x86)
)

set CHROME_EXE=%TARGET_DIR%\chrome-headless-shell.exe

echo    Download: Chrome Headless Shell %ARCH%
echo    Dimensione attesa: ~30-50 MB

REM 2. Crea struttura directory
echo.
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

if not exist "%TARGET_DIR%" (
    mkdir "%TARGET_DIR%"
    echo ✅ Creata directory: %TARGET_DIR%\
) else (
    echo ✅ Directory esistente: %TARGET_DIR%\
)

REM Verifica creazione riuscita
if not exist "%TARGET_DIR%" (
    echo ❌ Errore: impossibile creare %TARGET_DIR%\
    echo    Verifica i permessi e riprova
    pause
    exit /b 1
)

REM 3. Controlla se Chrome Headless Shell è già presente
if exist "%CHROME_EXE%" (
    echo.
    echo ✅ Chrome Headless Shell già presente in %TARGET_DIR%\

    REM Test veloce
    echo 🧪 Test Chrome Headless Shell esistente...
    "%CHROME_EXE%" --version >nul 2>&1
    if %ERRORLEVEL%==0 (
        for /f "tokens=*" %%i in ('"%CHROME_EXE%" --version 2^>nul') do (
            echo ✅ Chrome Headless Shell funziona: %%i
        )
        echo 🎉 Setup già completato!
        echo.
        echo Per reinstallare: elimina la cartella %TARGET_DIR%\ e rilancia lo script
        pause
        exit /b 0
    ) else (
        echo ⚠️  Chrome presente ma non funziona, reinstallo...
    )
)

REM 4. Download Chrome Headless Shell (VERSIONE VELOCE)
echo.
echo ⬇️  Scaricando Chrome Headless Shell %ARCH%...
echo    URL: %CHROME_URL%
echo    Dimensione: ~30-50 MB
echo    Destinazione: %TARGET_DIR%\

REM Elimina eventuali file precedenti
if exist chrome-headless-shell.zip del chrome-headless-shell.zip >nul 2>&1

REM Prova prima curl (più veloce), poi PowerShell come fallback
echo 🚀 Tentativo download con curl...
curl --version >nul 2>&1
if %ERRORLEVEL%==0 (
    echo ✅ curl disponibile - download veloce!
    curl -L --progress-bar -o chrome-headless-shell.zip "%CHROME_URL%"
    if %ERRORLEVEL%==0 (
        echo ✅ Download completato con curl
        goto :download_success
    ) else (
        echo ⚠️  curl fallito, provo con PowerShell...
    )
) else (
    echo ⚠️  curl non disponibile, uso PowerShell...
)

REM Fallback con PowerShell
echo 🐌 Download con PowerShell...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Write-Host 'Download Chrome Headless Shell in corso...'; try { Invoke-WebRequest -Uri '%CHROME_URL%' -OutFile 'chrome-headless-shell.zip' -UseBasicParsing; Write-Host 'Download completato' } catch { Write-Host 'Errore download:' $_.Exception.Message; exit 1 }}"

if %ERRORLEVEL% neq 0 (
    echo ❌ Errore nel download di Chrome Headless Shell
    echo    Possibili cause:
    echo    - Connessione internet assente
    echo    - Firewall/antivirus blocca il download
    echo    - URL temporaneamente non disponibile
    if exist chrome-headless-shell.zip del chrome-headless-shell.zip
    pause
    exit /b 1
)

:download_success
if not exist chrome-headless-shell.zip (
    echo ❌ File chrome-headless-shell.zip non trovato dopo il download
    pause
    exit /b 1
)

REM 5. Verifica integrità file
echo.
echo 🔍 Verifica integrità file scaricato...

REM Controlla dimensione file (Headless Shell è più piccolo: ~10MB minimo)
for %%I in (chrome-headless-shell.zip) do set FILE_SIZE=%%~zI
if %FILE_SIZE% LSS 10971520 (
    echo ❌ File chrome-headless-shell.zip troppo piccolo ^(%FILE_SIZE% bytes^)
    echo    Il download potrebbe essere incompleto o corrotto
    echo    Riprova il download
    if exist chrome-headless-shell.zip del chrome-headless-shell.zip
    pause
    exit /b 1
)

set /a FILE_SIZE_MB=%FILE_SIZE% / 1048576
echo ✅ Dimensione file: %FILE_SIZE_MB% MB

REM Test ZIP validity
powershell -Command "try { Add-Type -AssemblyName System.IO.Compression.FileSystem; [System.IO.Compression.ZipFile]::OpenRead('chrome-headless-shell.zip').Dispose(); Write-Host '✅ File ZIP valido' } catch { Write-Host '❌ File ZIP corrotto'; exit 1 }"

if %ERRORLEVEL% neq 0 (
    echo ❌ File chrome-headless-shell.zip corrotto o non valido
    echo    Possibili cause:
    echo    - Download interrotto
    echo    - Connessione instabile
    echo    - Antivirus ha modificato il file
    echo.
    echo 💡 Soluzioni:
    echo    1. Riprova il download
    echo    2. Disabilita temporaneamente l'antivirus
    echo    3. Usa una connessione diversa
    if exist chrome-headless-shell.zip del chrome-headless-shell.zip
    pause
    exit /b 1
)

REM 6. Estrazione Chrome Headless Shell
echo.
echo 📦 Estraendo Chrome Headless Shell...

powershell -Command "try { Expand-Archive -Path 'chrome-headless-shell.zip' -DestinationPath '.' -Force; Write-Host 'Estrazione completata' } catch { Write-Host 'Errore estrazione:' $_.Exception.Message; exit 1 }"

if %ERRORLEVEL% neq 0 (
    echo ❌ Errore nell'estrazione
    if exist chrome-headless-shell.zip del chrome-headless-shell.zip
    pause
    exit /b 1
)

REM Sposta file dalla cartella estratta alla directory target
if exist "%EXTRACT_DIR%" (
    echo 📁 Spostando file Chrome Headless Shell in %TARGET_DIR%\...
    xcopy "%EXTRACT_DIR%\*" "%TARGET_DIR%\" /E /I /Y >nul 2>&1
    if %ERRORLEVEL%==0 (
        rmdir "%EXTRACT_DIR%" /S /Q >nul 2>&1
        echo ✅ File spostati con successo
    ) else (
        echo ❌ Errore nello spostamento file
        pause
        exit /b 1
    )
) else (
    echo ❌ Cartella %EXTRACT_DIR% non trovata dopo estrazione
    pause
    exit /b 1
)

REM Pulisci file zip
del chrome-headless-shell.zip >nul 2>&1

REM 7. Verifica installazione finale
echo.
echo 🔍 Verifica installazione...

if exist "%CHROME_EXE%" (
    echo ✅ Chrome Headless Shell installato: %CHROME_EXE%

    REM Test finale
    echo 🧪 Test funzionalità Chrome Headless Shell...
    "%CHROME_EXE%" --version >nul 2>&1
    if %ERRORLEVEL%==0 (
        for /f "tokens=*" %%i in ('"%CHROME_EXE%" --version 2^>nul') do (
            echo ✅ Chrome Headless Shell funziona perfettamente: %%i
        )
        echo.
        echo 🎉 Setup completato con successo!
        echo 📍 Chrome Headless Shell installato in: %CD%\%TARGET_DIR%\
        echo 🚀 Il converter è pronto per l'uso! (versione leggera ~%FILE_SIZE_MB%MB)
    ) else (
        echo ⚠️  Chrome Headless Shell installato ma test fallito
        echo    ^(Normale su alcuni sistemi, dovrebbe funzionare nell'applicazione^)
        echo.
        echo 🎉 Setup completato!
    ) else (
    echo ❌ Chrome Headless Shell non trovato dopo l'installazione
    echo    Qualcosa è andato storto durante l'estrazione
    pause
    exit /b 1
)

echo.
echo 📋 Prossimi passi:
echo    1. Compila il progetto Java
echo    2. Esegui l'applicazione
echo    3. Chrome Headless Shell verrà rilevato automaticamente
echo.
echo 💡 Vantaggi Headless Shell:
echo    - Molto più leggero (~%FILE_SIZE_MB%MB vs ~150MB Chrome completo)
echo    - Avvio più veloce
echo    - Stesso risultato PDF identico
echo.
echo 🚀 Nota: Questo script prova prima curl (veloce) poi PowerShell (compatibile)
echo.
pause