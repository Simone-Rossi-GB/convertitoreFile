#!/bin/bash
# setup_chrome.sh - Download Chrome/Chromium per Linux e macOS

echo ""
echo "🚀 Chrome Setup per Email Converter"
echo "==================================="

# 1. Rileva sistema operativo
OS=$(uname -s)
ARCH=$(uname -m)

case $OS in
    "Darwin")
        OS_TYPE="mac"
        TARGET_DIR="lib/mac"
        EXECUTABLE="Google Chrome for Testing.app"
        CHROME_URL="https://storage.googleapis.com/chrome-for-testing-public/stable/mac-x64/chrome-mac-x64.zip"
        echo "🍎 Rilevato: macOS ($ARCH)"
        ;;
    "Linux")
        OS_TYPE="linux"
        TARGET_DIR="lib/linux"
        EXECUTABLE="chrome"
        CHROME_URL="https://storage.googleapis.com/chrome-for-testing-public/stable/linux64/chrome-linux64.zip"
        echo "🐧 Rilevato: Linux ($ARCH)"
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
for dir in "lib/windows" "lib/mac"; do
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

# 3. Controlla se Chrome è già presente
CHROME_PATH="$TARGET_DIR/$EXECUTABLE"

if [ "$OS_TYPE" = "mac" ]; then
    CHROME_EXEC="$CHROME_PATH/Contents/MacOS/Google Chrome for Testing"
else
    CHROME_EXEC="$CHROME_PATH"
fi

if [ -e "$CHROME_PATH" ]; then
    echo ""
    echo "✅ Chrome già presente in $TARGET_DIR/"

    # Test veloce di Chrome esistente
    echo "🧪 Test Chrome esistente..."
    if [ -x "$CHROME_EXEC" ]; then
        VERSION=$("$CHROME_EXEC" --version 2>/dev/null | head -1)
        if [ $? -eq 0 ] && [ -n "$VERSION" ]; then
            echo "✅ Chrome funziona: $VERSION"
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
    DOWNLOAD_CMD="curl -L --progress-bar -o chrome.zip"
    echo "✅ Trovato: curl"
elif command -v wget >/dev/null 2>&1; then
    DOWNLOAD_CMD="wget --progress=bar:force -O chrome.zip"
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

# 5. Download Chrome
echo ""
echo "⬇️  Scaricando Chrome per $OS_TYPE..."
echo "   URL: $CHROME_URL"
echo "   Dimensione: ~150-200 MB"
echo "   Destinazione: $TARGET_DIR/"

$DOWNLOAD_CMD "$CHROME_URL"

if [ $? -ne 0 ]; then
    echo "❌ Errore nel download di Chrome"
    echo "   Possibili cause:"
    echo "   - Connessione internet assente"
    echo "   - URL temporaneamente non disponibile"
    echo "   - Firewall blocca il download"
    rm -f chrome.zip
    exit 1
fi

if [ ! -f "chrome.zip" ]; then
    echo "❌ File chrome.zip non trovato dopo il download"
    exit 1
fi

# 6. Estrazione Chrome
echo ""
echo "📦 Estraendo Chrome..."

unzip -q chrome.zip

if [ $? -ne 0 ]; then
    echo "❌ Errore nell'estrazione"
    rm -f chrome.zip
    exit 1
fi

# Sposta file dalla cartella estratta alla directory target
case $OS_TYPE in
    "mac")
        if [ -d "chrome-mac-x64" ]; then
            echo "📁 Spostando Chrome app in $TARGET_DIR/..."
            mv "chrome-mac-x64/Google Chrome for Testing.app" "$TARGET_DIR/"
            rmdir "chrome-mac-x64"
            echo "✅ File spostati con successo"
        else
            echo "❌ Cartella chrome-mac-x64 non trovata dopo estrazione"
            rm -f chrome.zip
            exit 1
        fi
        ;;
    "linux")
        if [ -d "chrome-linux64" ]; then
            echo "📁 Spostando file Chrome in $TARGET_DIR/..."
            mv chrome-linux64/* "$TARGET_DIR/"
            rmdir "chrome-linux64"
            echo "✅ File spostati con successo"
        else
            echo "❌ Cartella chrome-linux64 non trovata dopo estrazione"
            rm -f chrome.zip
            exit 1
        fi
        ;;
esac

# Pulisci file zip
rm -f chrome.zip

# 7. Configura permessi
echo ""
echo "🔧 Configurando permessi..."

if [ "$OS_TYPE" = "mac" ]; then
    # Per macOS: imposta permessi eseguibili e rimuovi quarantena
    chmod +x "$CHROME_EXEC"
    echo "✅ Permessi eseguibili impostati"

    echo "🔓 Rimuovendo quarantena macOS..."
    xattr -dr com.apple.quarantine "$CHROME_PATH" 2>/dev/null || true
    echo "✅ Quarantena rimossa"
else
    # Per Linux: imposta permessi eseguibili
    chmod +x "$CHROME_EXEC"
    echo "✅ Permessi eseguibili impostati"
fi

# 8. Verifica installazione finale
echo ""
echo "🔍 Verifica installazione..."

if [ -e "$CHROME_PATH" ]; then
    echo "✅ Chrome installato: $CHROME_PATH"

    # Test finale
    echo "🧪 Test funzionalità Chrome..."
    VERSION=$("$CHROME_EXEC" --version 2>/dev/null | head -1)

    if [ $? -eq 0 ] && [ -n "$VERSION" ]; then
        echo "✅ Chrome funziona perfettamente: $VERSION"
        echo ""
        echo "🎉 Setup completato con successo!"
        echo "📍 Chrome installato in: $(pwd)/$TARGET_DIR/"
        echo "🚀 Il converter è pronto per l'uso!"
    else
        echo "⚠️  Chrome installato ma test fallito"
        echo "   (Normale su alcuni sistemi, dovrebbe funzionare nell'applicazione)"
        echo ""
        echo "🎉 Setup completato!"
    fi
else
    echo "❌ Chrome non trovato dopo l'installazione"
    echo "   Qualcosa è andato storto durante l'estrazione"
    exit 1
fi

echo ""
echo "📋 Prossimi passi:"
echo "   1. Compila il progetto Java"
echo "   2. Esegui l'applicazione"
echo "   3. Chrome verrà rilevato automaticamente"

if [ "$OS_TYPE" = "mac" ]; then
    echo ""
    echo "💡 Nota macOS:"
    echo "   Al primo avvio, macOS potrebbe chiedere autorizzazione"
    echo "   per eseguire Chrome. Clicca 'Apri' quando richiesto."
fi

echo ""