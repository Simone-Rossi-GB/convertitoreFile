# setup_chrome.ps1 - Download Chrome Headless Shell per Windows (PowerShell)

# Imposta titolo finestra
$Host.UI.RawUI.WindowTitle = "Setup Chrome Headless Shell per Email Converter"

Write-Host ""
Write-Host "🚀 Chrome Headless Shell Setup per Email Converter" -ForegroundColor Green
Write-Host "=================================================" -ForegroundColor Green
Write-Host "🪟 Windows rilevato (PowerShell)" -ForegroundColor Yellow

# 1. Rileva architettura Windows
Write-Host ""
Write-Host "🔍 Rilevamento architettura..." -ForegroundColor Blue

$OSArch = [Environment]::GetEnvironmentVariable("PROCESSOR_ARCHITECTURE")
$OSArchW6432 = [Environment]::GetEnvironmentVariable("PROCESSOR_ARCHITEW6432")

if ($OSArch -eq "AMD64" -or $OSArchW6432 -eq "AMD64") {
    $Arch = "x64"
    $TargetDir = "lib\windows"
    $ChromeUrl = "https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/win64/chrome-headless-shell-win64.zip"
    $ExtractDir = "chrome-headless-shell-win64"
    Write-Host "✅ Architettura: 64-bit (x64)" -ForegroundColor Green
} else {
    $Arch = "x86"
    $TargetDir = "lib\windows"
    $ChromeUrl = "https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/win32/chrome-headless-shell-win32.zip"
    $ExtractDir = "chrome-headless-shell-win32"
    Write-Host "✅ Architettura: 32-bit (x86)" -ForegroundColor Green
}

$ChromeExe = "$TargetDir\chrome-headless-shell.exe"
$ZipFile = "chrome-headless-shell.zip"

Write-Host "   Download: Chrome Headless Shell $Arch" -ForegroundColor Gray
Write-Host "   Dimensione attesa: ~30-50 MB" -ForegroundColor Gray

# 2. Crea struttura directory
Write-Host ""
Write-Host "📂 Verificando/creando struttura directory..." -ForegroundColor Blue

$directories = @("lib", "lib\windows")

foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        try {
            New-Item -ItemType Directory -Path $dir -Force | Out-Null
            Write-Host "✅ Creata directory: $dir\" -ForegroundColor Green
        }
        catch {
            Write-Host "❌ Errore creando directory $dir\: $($_.Exception.Message)" -ForegroundColor Red
            Read-Host "Premi Enter per uscire"
            exit 1
        }
    }
    else {
        Write-Host "✅ Directory esistente: $dir\" -ForegroundColor Green
    }
}

# Verifica creazione riuscita
if (!(Test-Path $TargetDir)) {
    Write-Host "❌ Errore: impossibile creare $TargetDir\" -ForegroundColor Red
    Write-Host "   Verifica i permessi e riprova" -ForegroundColor Gray
    Read-Host "Premi Enter per uscire"
    exit 1
}

# 3. Controlla se Chrome Headless Shell è già presente
if (Test-Path $ChromeExe) {
    Write-Host ""
    Write-Host "✅ Chrome Headless Shell già presente in $TargetDir\" -ForegroundColor Green
    
    # Test veloce
    Write-Host "🧪 Test Chrome Headless Shell esistente..." -ForegroundColor Blue
    try {
        $versionOutput = & $ChromeExe --version 2>$null
        if ($LASTEXITCODE -eq 0 -and $versionOutput) {
            Write-Host "✅ Chrome Headless Shell funziona: $versionOutput" -ForegroundColor Green
            Write-Host "🎉 Setup già completato!" -ForegroundColor Green
            Write-Host ""
            Write-Host "Per reinstallare: elimina la cartella $TargetDir\ e rilancia lo script" -ForegroundColor Gray
            Read-Host "Premi Enter per continuare"
            exit 0
        }
        else {
            Write-Host "⚠️  Chrome presente ma non funziona, reinstallo..." -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "⚠️  Chrome presente ma test fallito, reinstallo..." -ForegroundColor Yellow
    }
}

# 4. Download Chrome Headless Shell
Write-Host ""
Write-Host "⬇️  Scaricando Chrome Headless Shell $Arch..." -ForegroundColor Blue

Write-Host "   URL: $ChromeUrl" -ForegroundColor Gray
Write-Host "   Dimensione: ~30-50 MB (Headless Shell è molto più leggero!)" -ForegroundColor Gray
Write-Host "   Destinazione: $TargetDir\" -ForegroundColor Gray

try {
    # Forza TLS 1.2 per compatibilità
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    
    # Rimuovi file precedenti
    if (Test-Path $ZipFile) {
        Remove-Item $ZipFile -Force
        Write-Host "🗑️  Rimosso file precedente" -ForegroundColor Gray
    }
    
    # Progress bar per download
    Write-Host "📥 Download Chrome Headless Shell in corso..." -ForegroundColor Blue
    $ProgressPreference = 'Continue'
    Invoke-WebRequest -Uri $ChromeUrl -OutFile $ZipFile -UseBasicParsing
    
    Write-Host "✅ Download completato" -ForegroundColor Green
}
catch {
    Write-Host "❌ Errore nel download di Chrome Headless Shell: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Possibili cause:" -ForegroundColor Yellow
    Write-Host "- Connessione internet assente" -ForegroundColor Gray
    Write-Host "- Firewall/antivirus blocca il download" -ForegroundColor Gray
    Write-Host "- URL temporaneamente non disponibile" -ForegroundColor Gray
    
    if (Test-Path $ZipFile) {
        Remove-Item $ZipFile -Force
    }
    Read-Host "Premi Enter per uscire"
    exit 1
}

if (!(Test-Path $ZipFile)) {
    Write-Host "❌ File chrome-headless-shell.zip non trovato dopo il download" -ForegroundColor Red
    Read-Host "Premi Enter per uscire"
    exit 1
}

# 5. Verifica integrità file
Write-Host ""
Write-Host "🔍 Verifica integrità file scaricato..." -ForegroundColor Blue

$FileSize = (Get-Item $ZipFile).Length

# Headless Shell è più piccolo: minimo 10MB
if ($FileSize -lt 10971520) {
    Write-Host "❌ File chrome-headless-shell.zip troppo piccolo ($FileSize bytes)" -ForegroundColor Red
    Write-Host "   Il download potrebbe essere incompleto o corrotto" -ForegroundColor Gray
    Write-Host "   Riprova il download" -ForegroundColor Gray
    Remove-Item $ZipFile -Force
    Read-Host "Premi Enter per uscire"
    exit 1
}

$FileSizeMB = [math]::Round($FileSize / 1MB, 1)
Write-Host "✅ Dimensione file: $FileSizeMB MB" -ForegroundColor Green

# Test ZIP validity
try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::OpenRead($ZipFile).Dispose()
    Write-Host "✅ File ZIP valido" -ForegroundColor Green
}
catch {
    Write-Host "❌ File chrome-headless-shell.zip corrotto o non valido" -ForegroundColor Red
    Write-Host "   Possibili cause:" -ForegroundColor Yellow
    Write-Host "   - Download interrotto" -ForegroundColor Gray
    Write-Host "   - Connessione instabile" -ForegroundColor Gray
    Write-Host "   - Antivirus ha modificato il file" -ForegroundColor Gray
    Write-Host ""
    Write-Host "💡 Soluzioni:" -ForegroundColor Yellow
    Write-Host "   1. Riprova il download" -ForegroundColor Gray
    Write-Host "   2. Disabilita temporaneamente l'antivirus" -ForegroundColor Gray
    Write-Host "   3. Usa una connessione diversa" -ForegroundColor Gray
    Remove-Item $ZipFile -Force
    Read-Host "Premi Enter per uscire"
    exit 1
}

# 6. Estrazione Chrome Headless Shell
Write-Host ""
Write-Host "📦 Estraendo Chrome Headless Shell..." -ForegroundColor Blue

try {
    Expand-Archive -Path $ZipFile -DestinationPath "." -Force
    Write-Host "✅ Estrazione completata" -ForegroundColor Green
}
catch {
    Write-Host "❌ Errore nell'estrazione: $($_.Exception.Message)" -ForegroundColor Red
    if (Test-Path $ZipFile) {
        Remove-Item $ZipFile -Force
    }
    Read-Host "Premi Enter per uscire"
    exit 1
}

# Sposta file dalla cartella estratta alla directory target
if (Test-Path $ExtractDir) {
    Write-Host "📁 Spostando file Chrome Headless Shell in $TargetDir\..." -ForegroundColor Blue
    try {
        Get-ChildItem $ExtractDir | Move-Item -Destination $TargetDir -Force
        Remove-Item $ExtractDir -Recurse -Force
        Write-Host "✅ File spostati con successo" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Errore nello spostamento file: $($_.Exception.Message)" -ForegroundColor Red
        Read-Host "Premi Enter per uscire"
        exit 1
    }
}
else {
    Write-Host "❌ Cartella $ExtractDir non trovata dopo estrazione" -ForegroundColor Red
    Read-Host "Premi Enter per uscire"
    exit 1
}

# Pulisci file zip
if (Test-Path $ZipFile) {
    Remove-Item $ZipFile -Force
}

# 7. Verifica installazione finale
Write-Host ""
Write-Host "🔍 Verifica installazione..." -ForegroundColor Blue

if (Test-Path $ChromeExe) {
    Write-Host "✅ Chrome Headless Shell installato: $ChromeExe" -ForegroundColor Green
    
    # Test finale
    Write-Host "🧪 Test funzionalità Chrome Headless Shell..." -ForegroundColor Blue
    try {
        $versionOutput = & $ChromeExe --version 2>$null
        if ($LASTEXITCODE -eq 0 -and $versionOutput) {
            Write-Host "✅ Chrome Headless Shell funziona perfettamente: $versionOutput" -ForegroundColor Green
            Write-Host ""
            Write-Host "🎉 Setup completato con successo!" -ForegroundColor Green
            Write-Host "📍 Chrome Headless Shell installato in: $(Get-Location)\$TargetDir\" -ForegroundColor Green
            Write-Host "🚀 Il converter è pronto per l'uso! (versione leggera ~$FileSizeMB MB)" -ForegroundColor Green
        }
        else {
            Write-Host "⚠️  Chrome Headless Shell installato ma test fallito" -ForegroundColor Yellow
            Write-Host "   (Normale su alcuni sistemi, dovrebbe funzionare nell'applicazione)" -ForegroundColor Gray
            Write-Host ""
            Write-Host "🎉 Setup completato!" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "⚠️  Chrome Headless Shell installato ma test fallito: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "   (Dovrebbe funzionare nell'applicazione)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "🎉 Setup completato!" -ForegroundColor Green
    }
}
else {
    Write-Host "❌ Chrome Headless Shell non trovato dopo l'installazione" -ForegroundColor Red
    Write-Host "   Qualcosa è andato storto durante l'estrazione" -ForegroundColor Gray
    Read-Host "Premi Enter per uscire"
    exit 1
}

# 8. Informazioni finali
Write-Host ""
Write-Host "📋 Prossimi passi:" -ForegroundColor Cyan
Write-Host "   1. Compila il progetto Java" -ForegroundColor Gray
Write-Host "   2. Esegui l'applicazione" -ForegroundColor Gray
Write-Host "   3. Chrome Headless Shell verrà rilevato automaticamente" -ForegroundColor Gray

Write-Host ""
Write-Host "💡 Vantaggi Headless Shell:" -ForegroundColor Cyan
Write-Host "   - Molto più leggero (~$FileSizeMB MB vs ~150MB Chrome completo)" -ForegroundColor Gray
Write-Host "   - Avvio più veloce" -ForegroundColor Gray
Write-Host "   - Meno RAM utilizzata" -ForegroundColor Gray
Write-Host "   - Stesso risultato PDF identico" -ForegroundColor Gray
Write-Host "   - Specifico per automazione (nessuna GUI)" -ForegroundColor Gray

Write-Host ""
Write-Host "🔄 Per aggiornare in futuro:" -ForegroundColor Cyan
Write-Host "   - Elimina lib\windows\ e rilancia lo script" -ForegroundColor Gray
Write-Host "   - Lo script scaricherà automaticamente l'ultima versione" -ForegroundColor Gray

Write-Host ""
Read-Host "Premi Enter per continuare"
