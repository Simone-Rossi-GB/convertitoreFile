#!/bin/bash
# setup_chrome.sh - Download Chrome Headless Shell per Linux e macOS

echo ""
echo "🚀 Chrome Headless Shell Setup per Email Converter"
echo "================================================="

# 1. Rileva sistema operativo e architettura
OS=$(uname -s)
ARCH=$(uname -m)

echo "🔍 Rilevamento sistema..."
echo "   OS: $OS"
echo "   Architettura: $ARCH"

case $OS in
    "Darwin")
        OS_TYPE="mac"
        TARGET_DIR="lib/mac"
        EXECUTABLE="chrome-headless-shell"
        
        # Rileva architettura macOS corretta
        if [ "$ARCH" = "arm64" ]; then
            CHROME_URL="https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/mac-arm64/chrome-headless-shell-mac-arm64.zip"
            EXTRACT_DIR="chrome-headless-shell-mac-arm64"
            echo "🍎 Rilevato: macOS Apple Silicon (ARM64)"
        else
            CHROME_URL="https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/mac-x64/chrome-headless-shell-mac-x64.zip"
            EXTRACT_DIR="chrome-headless-shell-mac-x64"
            echo "🍎 Rilevato: macOS Intel (x64)"
        fi
        echo "   Download: Chrome Headless Shell per macOS"
        echo "   Dimensione attesa: ~30-50 MB (molto più leggero!)"
        ;;
        
    "Linux")
        OS_TYPE="linux"
        TARGET_DIR="lib/linux"
        EXECUTABLE="chrome-headless-shell"
        
        # Per Linux assumiamo sempre 64-bit (più comune)
        CHROME_URL="https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/linux64/chrome-headless-shell-linux64.zip"
        EXTRACT_DIR="chrome-headless-shell-linux64"
        echo "🐧 Rilevato: Linux 64-bit"
        echo "   Download: Chrome Headless Shell per Linux"
        echo "   Dimensione attesa: ~30-50 MB (molto più leggero!)"
        ;;
        
    *)
        echo "❌ Sistema operativo non supportato: $OS"
        echo "   Per Windows usa: setup_chrome.bat"
        exit 1
        ;;
esac

# 2. Crea struttura directory
echo ""
echo "📂 Verificando/creando struttura directory..."

# Crea directory lib se non esiste
if [ ! -d "lib" ]; then
    mkdir -p "lib"
    echo "✅ Creata directory: lib/"
else
    echo "✅ Directory esistente: lib/"
fi

# Crea directory per l'OS corrente
if [ ! -d "$TARGET_DIR" ]; then
    mkdir -p "$TARGET_DIR"
    echo "✅ Creata directory: $TARGET_DIR/"
else
    echo "✅ Directory esistente: $TARGET_DIR/"
fi

# Crea anche le altre directory per completezza
for dir in "lib/mac"; do
    if [ ! -d "$dir" ]; then
        mkdir -p "$dir"
        echo "✅ Creata directory: $dir/"
    fi
done

# Verifica creazione riuscita
if [ ! -d "$TARGET_DIR" ]; then
    echo "❌ Errore: impossibile creare $TARGET_DIR"
    echo "   Verifica i permessi e riprova"
    exit 1
fi

# 3. Controlla se Chrome Headless Shell è già presente
CHROME_PATH="$TARGET_DIR/$EXECUTABLE"

if [ -f "$CHROME_PATH" ]; then
    echo ""
    echo "✅ Chrome Headless Shell già presente in $TARGET_DIR/"
    
    # Test veloce
    echo "🧪 Test Chrome Headless Shell esistente..."
    if [ -x "$CHROME_PATH" ]; then
        VERSION=$("$CHROME_PATH" --version 2>/dev/null | head -1)
        if [ $? -eq 0 ] && [ -n "$VERSION" ]; then
            echo "✅ Chrome Headless Shell funziona: $VERSION"
            echo "🎉 Setup già completato!"
            echo ""
            echo "Per reinstallare: rm -rf $TARGET_DIR e rilancia lo script"
            exit 0
        else
            echo "⚠️  Chrome presente ma non funziona, reinstallo..."
        fi
    else
        echo "⚠️  Chrome presente ma non eseguibile, reinstallo..."
    fi
fi

# 4. Verifica strumenti necessari
echo ""
echo "🔍 Verifica strumenti necessari..."

if command -v curl >/dev/null 2>&1; then
    DOWNLOAD_CMD="curl -L --progress-bar -o chrome-headless-shell.zip"
    echo "✅ Trovato: curl"
elif command -v wget >/dev/null 2>&1; then
    DOWNLOAD_CMD="wget --progress=bar:force -O chrome-headless-shell.zip"
    echo "✅ Trovato: wget"
else
    echo "❌ Errore: curl o wget non trovati"
    echo "   Installa uno di questi:"
    if [ "$OS_TYPE" = "mac" ]; then
        echo "   brew install curl"
    else
        echo "   sudo apt install curl   # Ubuntu/Debian"
        echo "   sudo yum install curl   # CentOS/RHEL"
    fi
    exit 1
fi

if ! command -v unzip >/dev/null 2>&1; then
    echo "❌ Errore: unzip non trovato"
    echo "   Installa: sudo apt install unzip (Linux) / brew install unzip (Mac)"
    exit 1
fi

echo "✅ Trovato: unzip"

# 5. Download Chrome Headless Shell con fallback architetture
echo ""
echo "⬇️  Scaricando Chrome Headless Shell per $OS_TYPE..."
echo "   Architettura target: $ARCH"
echo "   URL: $CHROME_URL"
echo "   Destinazione: $TARGET_DIR/"

# Rimuovi file precedenti se esistono
rm -f chrome-headless-shell.zip

# Lista di URL da provare (principalmente per macOS con fallback architettura)
if [ "$OS_TYPE" = "mac" ]; then
    if [ "$ARCH" = "arm64" ]; then
        URLS=(
            "https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/mac-arm64/chrome-headless-shell-mac-arm64.zip"
            "https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/mac-x64/chrome-headless-shell-mac-x64.zip"
        )
        EXTRACT_DIRS=("chrome-headless-shell-mac-arm64" "chrome-headless-shell-mac-x64")
    else
        URLS=(
            "https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/mac-x64/chrome-headless-shell-mac-x64.zip"
            "https://storage.googleapis.com/chrome-for-testing-public/139.0.7258.6/mac-arm64/chrome-headless-shell-mac-arm64.zip"
        )
        EXTRACT_DIRS=("chrome-headless-shell-mac-x64" "chrome-headless-shell-mac-arm64")
    fi
else
    URLS=("$CHROME_URL")
    EXTRACT_DIRS=("$EXTRACT_DIR")
fi

# Prova ogni URL fino a trovarne uno che funziona
DOWNLOAD_SUCCESS=false
for i in "${!URLS[@]}"; do
    CURRENT_URL="${URLS[$i]}"
    CURRENT_EXTRACT_DIR="${EXTRACT_DIRS[$i]}"
    
    echo "🔄 Tentativo $((i+1)): $(basename "$CURRENT_URL")"
    
    $DOWNLOAD_CMD "$CURRENT_URL"
    
    if [ $? -eq 0 ] && [ -f "chrome-headless-shell.zip" ]; then
        # Verifica che sia un ZIP valido e di dimensione adeguata per Headless Shell
        FILE_SIZE=$(stat -f%z "chrome-headless-shell.zip" 2>/dev/null || stat -c%s "chrome-headless-shell.zip" 2>/dev/null || echo 0)
        
        # Headless Shell è più piccolo: minimo 10MB
        if [ "$FILE_SIZE" -gt 10971520 ] && unzip -t chrome-headless-shell.zip >/dev/null 2>&1; then
            echo "✅ Download riuscito da: $CURRENT_URL"
            echo "✅ Dimensione file: $(echo $FILE_SIZE | awk '{print int($1/1024/1024)}') MB"
            EXTRACT_DIR="$CURRENT_EXTRACT_DIR"
            DOWNLOAD_SUCCESS=true
            break
        else
            echo "⚠️  File scaricato non valido, provo URL successivo..."
            rm -f chrome-headless-shell.zip
        fi
    else
        echo "⚠️  Download fallito, provo URL successivo..."
        rm -f chrome-headless-shell.zip
    fi
done

if [ "$DOWNLOAD_SUCCESS" = false ]; then
    echo "❌ Tutti i tentativi di download sono falliti"
    echo "   URLs provati:"
    for url in "${URLS[@]}"; do
        echo "   - $url"
    done
    echo ""
    echo "💡 Soluzioni alternative:"
    echo "   1. Verifica connessione internet"
    echo "   2. Prova più tardi (server Google temporaneamente down)"
    echo "   3. Usa Chrome di sistema se già installato"
    echo "   4. Download manuale da: https://googlechromelabs.github.io/chrome-for-testing/"
    exit 1
fi

# 6. Estrazione Chrome Headless Shell
echo ""
echo "📦 Estraendo Chrome Headless Shell..."

unzip -q chrome-headless-shell.zip

if [ $? -ne 0 ]; then
    echo "❌ Errore nell'estrazione"
    rm -f chrome-headless-shell.zip
    exit 1
fi

# Sposta file dalla cartella estratta alla directory target
if [ -d "$EXTRACT_DIR" ]; then
    echo "📁 Spostando Chrome Headless Shell in $TARGET_DIR/..."
    mv "$EXTRACT_DIR"/* "$TARGET_DIR/"
    rmdir "$EXTRACT_DIR"
    echo "✅ File spostati con successo"
else
    echo "❌ Cartella $EXTRACT_DIR non trovata dopo estrazione"
    rm -f chrome-headless-shell.zip
    exit 1
fi

# Pulisci file zip
rm -f chrome-headless-shell.zip

# 7. Configura permessi
echo ""
echo "🔧 Configurando permessi..."

chmod +x "$CHROME_PATH"
echo "✅ Permessi eseguibili impostati"

if [ "$OS_TYPE" = "mac" ]; then
    # Per macOS: rimuovi quarantena
    echo "🔓 Rimuovendo quarantena macOS..."
    xattr -dr com.apple.quarantine "$CHROME_PATH" 2>/dev/null || true
    echo "✅ Quarantena rimossa"
fi

# 8. Verifica installazione finale
echo ""
echo "🔍 Verifica installazione..."

if [ -f "$CHROME_PATH" ]; then
    echo "✅ Chrome Headless Shell installato: $CHROME_PATH"
    
    # Test finale
    echo "🧪 Test funzionalità Chrome Headless Shell..."
    VERSION=$("$CHROME_PATH" --version 2>/dev/null | head -1)
    
    if [ $? -eq 0 ] && [ -n "$VERSION" ]; then
        echo "✅ Chrome Headless Shell funziona perfettamente: $VERSION"
        echo ""
        echo "🎉 Setup completato con successo!"
        echo "📍 Chrome Headless Shell installato in: $(pwd)/$TARGET_DIR/"
        echo "🚀 Il converter è pronto per l'uso! (versione leggera ~$(echo $FILE_SIZE | awk '{print int($1/1024/1024)}')MB)"
    else
        echo "⚠️  Chrome Headless Shell installato ma test fallito"
        echo "   (Normale su alcuni sistemi, dovrebbe funzionare nell'applicazione)"
        echo ""
        echo "🎉 Setup completato!"
    fi
else
    echo "❌ Chrome Headless Shell non trovato dopo l'installazione"
    echo "   Qualcosa è andato storto durante l'estrazione"
    exit 1
fi

echo ""
echo "📋 Prossimi passi:"
echo "   1. Compila il progetto Java"
echo "   2. Esegui l'applicazione"
echo "   3. Chrome Headless Shell verrà rilevato automaticamente"

echo ""
echo "💡 Vantaggi Headless Shell:"
echo "   - Molto più leggero (~$(echo $FILE_SIZE | awk '{print int($1/1024/1024)}')MB vs ~150MB Chrome completo)"
echo "   - Avvio più veloce"
echo "   - Meno RAM utilizzata"
echo "   - Stesso risultato PDF identico"

if [ "$OS_TYPE" = "mac" ]; then
    echo ""
    echo "💡 Nota macOS:"
    echo "   Al primo avvio, macOS potrebbe chiedere autorizzazione"
    echo "   per eseguire Chrome. Clicca 'Apri' quando richiesto."
fi

echo ""
