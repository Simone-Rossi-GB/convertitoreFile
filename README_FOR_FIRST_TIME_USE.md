# 🚀 Email to PDF Converter

Converter avanzato per trasformare email (EML/MSG) in PDF usando Chrome headless.

## 📋 Setup Iniziale

**Prima di usare l'applicazione, devi scaricare Chrome:**

### Windows 🪟
Scegli uno dei metodi:

**Metodo 1 - Batch (Semplice):**
```bash
setup_chrome.bat
```

**Metodo 2 - PowerShell (Raccomandato):**
```powershell
powershell -ExecutionPolicy Bypass -File setup_chrome.ps1
```

### macOS 🍎
```bash
chmod +x setup_chrome.sh
./setup_chrome.sh
```

### Linux 🐧
```bash
chmod +x setup_chrome.sh
./setup_chrome.sh
```

## 📁 Struttura dopo Setup

```
your-project/
├── lib/
│   ├── windows/           # Chrome per Windows (~150 MB)
│   │   ├── chrome.exe
│   │   └── ... (file Chrome)
│   ├── mac/              # Chrome per macOS (~200 MB)
│   │   └── Google Chrome for Testing.app
│   └── linux/            # Chrome per Linux (~160 MB)
│       ├── chrome
│       └── ... (file Chrome)
├── src/                  # Codice sorgente
├── setup_chrome.sh       # Script setup Unix
├── setup_chrome.bat      # Script setup Windows
├── setup_chrome.ps1      # Script setup PowerShell
└── README.md
```

## 🛠️ Utilizzo

1. **Esegui setup Chrome** (una sola volta)
2. **Compila progetto** con Maven/IDE
3. **Avvia applicazione** - Chrome verrà rilevato automaticamente

## ⚡ Conversioni Supportate

- ✅ **EML → PDF** (Outlook, Thunderbird, ecc.)
- ✅ **MSG → PDF** (Outlook .msg files)

## 🔧 Troubleshooting

### Chrome non trovato
```bash
# Rilancia setup
./setup_chrome.sh    # Unix
setup_chrome.bat     # Windows
```

### Permessi macOS
```bash
# Rimuovi quarantena
xattr -dr com.apple.quarantine lib/mac/Google\ Chrome\ for\ Testing.app
```

### Antivirus Windows
- Alcuni antivirus bloccano il download
- Disabilita temporaneamente durante setup
- Aggiungi cartella `lib/` alle esclusioni

## 📦 Download Size

- **Script setup**: ~2 KB
- **Repository Git**: ~20 MB (solo codice)
- **Chrome download**: 150-200 MB per OS
- **Totale finale**: ~170-220 MB

## 🔄 Aggiornamento Chrome

Per aggiornare Chrome:
```bash
# Elimina cartella esistente
rm -rf lib/windows  # o lib/mac, lib/linux

# Rilancia setup
./setup_chrome.sh
```

## 💡 Note

- Chrome viene scaricato da Google ufficiale
- **Nessuna modifica** ai file Chrome originali
- **Offline-ready** dopo il primo setup
- **Multi-platform** automatico