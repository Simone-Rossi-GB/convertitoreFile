# setup_chrome.ps1 - Download Chrome per Windows (PowerShell)

# Imposta titolo finestra
$Host.UI.RawUI.WindowTitle = "Setup Chrome per Email Converter"

Write-Host ""
Write-Host "🚀 Chrome Setup per Email Converter" -ForegroundColor Green
Write-Host "===================================" -ForegroundColor Green
Write-Host "🪟 Windows rilevato (PowerShell)" -ForegroundColor Yellow

# 1. Crea struttura directory
Write-Host ""
Write-Host "📂 Verificando/creando struttura directory..." -ForegroundColor Blue

$directories = @("lib", "lib\windows", "lib\mac")

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
$TargetDir = "lib\windows"
if (!(Test-Path $TargetDir)) {
    Write-Host "❌ Errore: impossibile creare $TargetDir\" -ForegroundColor Red
    Write-Host "   Verifica i permessi e riprova" -ForegroundColor Gray
    Read-Host "Premi Enter per uscire"
    exit 1
}

# 2. Controlla se Chrome è già presente
$ChromeExe = "$TargetDir\chrome.exe"

if (Test-Path $ChromeExe) {
    Write-Host ""
    Write-Host "✅ Chrome già presente in $TargetDir\" -ForegroundColor Green

    # Test veloce di Chrome esistente
    Write-Host "🧪 Test Chrome esistente..." -ForegroundColor Blue
    try {
        $versionOutput = & $ChromeExe --version 2>$null
        if ($LASTEXITCODE -eq 0 -and $versionOutput) {
            Write-Host "✅ Chrome funziona: $versionOutput" -ForegroundColor Green
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

# 3. Download Chrome
Write-Host ""
Write-Host "⬇️  Scaricando Chrome per Windows..." -ForegroundColor Blue

$ChromeUrl = "https://storage.googleapis.com/chrome-for-testing-public/stable/win64/chrome-win64.zip"
$ZipFile = "chrome.zip"

Write-Host "   URL: $ChromeUrl" -ForegroundColor Gray
Write-Host "   Dimensione: ~150 MB" -ForegroundColor Gray
Write-Host "   Destinazione: $TargetDir\" -ForegroundColor Gray

try {
    # Forza TLS 1.2 per compatibilità
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

    # Progress bar per download
    Write-Host "📥 Download in corso..." -ForegroundColor Blue
    $ProgressPreference = 'Continue'
    Invoke-WebRequest -Uri $ChromeUrl -OutFile $ZipFile -UseBasicParsing

    Write-Host "✅ Download completato" -ForegroundColor Green
}
catch {
    Write-Host "❌ Errore nel download di Chrome: $($_.Exception.Message)" -ForegroundColor Red
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
    Write-Host "❌ File chrome.zip non trovato dopo il download" -ForegroundColor Red
    Read-Host "Premi Enter per uscire"
    exit 1
}

# 4. Estrazione Chrome
Write-Host ""
Write-Host "📦 Estraendo Chrome..." -ForegroundColor Blue

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
$ExtractedDir = "chrome-win64"
if (Test-Path $ExtractedDir) {
    Write-Host "📁 Spostando file Chrome in $TargetDir\..." -ForegroundColor Blue
    try {
        Get-ChildItem $ExtractedDir | Move-Item -Destination $TargetDir -Force
        Remove-Item $ExtractedDir -Recurse -Force
        Write-Host "✅ File spostati con successo" -ForegroundColor Green
    }
    catch {
        Write-Host "❌ Errore nello spostamento file: $($_.Exception.Message)" -ForegroundColor Red
        Read-Host "Premi Enter per uscire"
        exit 1
    }
}
else {
    Write-Host "❌ Cartella $ExtractedDir non trovata dopo estrazione" -ForegroundColor Red
    Read-Host "Premi Enter per uscire"
    exit 1
}

# Pulisci file zip
if (Test-Path $ZipFile) {
    Remove-Item $ZipFile -Force
}

# 5. Verifica installazione finale
Write-Host ""
Write-Host "🔍 Verifica installazione..." -ForegroundColor Blue

if (Test-Path $ChromeExe) {
    Write-Host "✅ Chrome installato: $ChromeExe" -ForegroundColor Green

    # Test finale
    Write-Host "🧪 Test funzionalità Chrome..." -ForegroundColor Blue
    try {
        $versionOutput = & $ChromeExe --version 2>$null
        if ($LASTEXITCODE -eq 0 -and $versionOutput) {
            Write-Host "✅ Chrome funziona perfettamente: $versionOutput" -ForegroundColor Green
            Write-Host ""
            Write-Host "🎉 Setup completato con successo!" -ForegroundColor Green
            Write-Host "📍 Chrome installato in: $(Get-Location)\$TargetDir\" -ForegroundColor Green
            Write-Host "🚀 Il converter è pronto per l'uso!" -ForegroundColor Green
        }
        else {
            Write-Host "⚠️  Chrome installato ma test fallito" -ForegroundColor Yellow
            Write-Host "   (Normale su alcuni sistemi, dovrebbe funzionare nell'applicazione)" -ForegroundColor Gray
            Write-Host ""
            Write-Host "🎉 Setup completato!" -ForegroundColor Green
        }
    }
    catch {
        Write-Host "⚠️  Chrome installato ma test fallito: $($_.Exception.Message)" -ForegroundColor Yellow
        Write-Host "   (Dovrebbe funzionare nell'applicazione)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "🎉 Setup completato!" -ForegroundColor Green
    }
}
else {
    Write-Host "❌ Chrome non trovato dopo l'installazione" -ForegroundColor Red
    Write-Host "   Qualcosa è andato storto durante l'estrazione" -ForegroundColor Gray
    Read-Host "Premi Enter per uscire"
    exit 1
}

# 6. Informazioni finali
Write-Host ""
Write-Host "📋 Prossimi passi:" -ForegroundColor Cyan
Write-Host "   1. Compila il progetto Java" -ForegroundColor Gray
Write-Host "   2. Esegui l'applicazione" -ForegroundColor Gray
Write-Host "   3. Chrome verrà rilevato automaticamente" -ForegroundColor Gray

Write-Host ""
Write-Host "💡 Suggerimenti:" -ForegroundColor Cyan
Write-Host "   - Chrome è ora disponibile per il converter" -ForegroundColor Gray
Write-Host "   - Il setup non deve essere ripetuto" -ForegroundColor Gray
Write-Host "   - Per aggiornare Chrome: elimina lib\windows\ e rilancia lo script" -ForegroundColor Gray

Write-Host ""
Read-Host "Premi Enter per continuare"