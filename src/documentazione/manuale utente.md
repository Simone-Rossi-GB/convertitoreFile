# 📁 Manuale Utente - File Converter Manager

## 🚀 Primo Avvio

### Requisiti di Sistema

- Java 8 o superiore
- Permessi di lettura/scrittura sulle cartelle che userai

---

## 🎯 Come Funziona l'Applicazione

File Converter Manager è un'applicazione che converte automaticamente i file da un formato all'altro. Funziona monitorando una cartella: quando inserisci un file, l'app lo rileva e ti chiede in che formato convertirlo.

### 🔄 Il Processo di Conversione

1. **Inserisci** il file nella cartella monitorata
2. **Scegli** il formato di destinazione dal popup
3. **Attendi** che l'applicazione converta il file
4. **Trova** il file convertito nella cartella "File Convertiti"

---

## 🖥️ Interfaccia Principale

### Indicatori di Stato

- **Punto Verde** (in alto a destra): Monitoraggio attivo ✅
- **Punto Rosso** (in alto a destra): Monitoraggio disattivato ❌
- **Log dell'Applicazione**: Mostra in tempo reale cosa sta facendo l'app

### Statistiche

L'applicazione tiene traccia di:

- **File Rilevati**: Quanti file hai inserito
- **Conversioni Riuscite**: Quanti file sono stati convertiti con successo
- **Conversioni Fallite**: Quanti file hanno avuto problemi

---

## 📂 Gestione delle Cartelle

L'applicazione usa tre cartelle principali:

### 📥 Cartella Monitorata (Input)

- Qui inserisci i file da convertire
- L'app controlla continuamente questa cartella
- **Accesso rapido**: Pulsante "Carica File"

### ✅ Cartella File Convertiti (Success)

- I file convertiti con successo finiscono qui
- **Accesso rapido**: Pulsante "File Convertiti"

### ❌ Cartella Conversioni Fallite (Error)

- I file che non si riescono a convertire finiscono qui
- **Accesso rapido**: Pulsante "Conversioni Fallite"

> 💡 **Consiglio**: Crea una cartella principale (es. "Conversioni") e al suo interno le tre sottocartelle per tenere tutto organizzato.

---

## 🛠️ Configurazione

### Configurazione Base (Pulsante "Config")

**Cartelle Personalizzate**

- Puoi cambiare il percorso di tutte e tre le cartelle
- Usa i pulsanti "Sfoglia..." per selezionare cartelle diverse

**Monitoraggio Automatico**

- Attiva/disattiva l'avvio automatico del monitoraggio
- Se attivo, al prossimo avvio l'app inizierà subito a monitorare

### Configurazione Conversioni (Pulsante "Conversion Config")

**Password per File Protetti**

- Se devi convertire PDF o documenti protetti da password
- Inserisci la password qui prima di iniziare la conversione
- 🔒 **Privacy**: La password è usata solo per la conversione, mai salvata o condivisa

**Unione Pagine PDF**

- Quando converti PDF in immagini
- **OFF**: Ogni pagina diventa un'immagine separata
- **ON**: Tutte le pagine diventano un'unica immagine lunga

**Output Zippato**

- **OFF**: Ricevi il file convertito direttamente
- **ON**: Il file convertito viene messo in un archivio ZIP
- Utile per ridurre le dimensioni o organizzare meglio i file

---

## 🔄 Come Convertire i File

### Conversione Singola

1. **Attiva il monitoraggio** (pulsante "Avvia Monitoraggio" se il punto è rosso)
2. **Clicca "Carica File"** per aprire la cartella monitorata
3. **Copia/sposta il tuo file** nella cartella
4. **Scegli il formato** dal popup che appare
5. **Attendi** - vedrai il progresso nel log
6. **Trova il file convertito** nella cartella "Success"

### Conversione Multipla

1. **Inserisci più file insieme** nella cartella monitorata
2. **Scegli il formato** per ogni file (popup separati)
3. L'app processerà tutti i file uno dopo l'altro
4. Tutti i file convertiti finiranno nella cartella "Success"

### Formati Supportati

- L'app rileva automaticamente il formato del file originale
- Ti mostra solo i formati di destinazione disponibili
- Puoi vedere tutte le conversioni possibili nella finestra "Config"

---

## ⚠️ Gestione Errori e Problemi

### Indicatori di Problemi

- **Punto rosso**: Il monitoraggio è spento - clicca "Avvia Monitoraggio"
- **Popup di errore**: Spiega brevemente cosa è andato storto
- **File nella cartella Error**: Conversioni fallite

### Cause Comuni di Errore

- File corrotto o non leggibile
- Formato non supportato per la conversione richiesta
- File troppo grande
- File protetto senza password corretta
- Problemi di connessione al server

### Cosa Fare in Caso di Problemi

**Se l'app va lenta o si blocca:**

1. Chiudi e riapri l'applicazione
2. Se persiste, riavvia il computer
3. Verifica di aver eseguito il file di primo avvio

**Se le conversioni falliscono sempre:**

1. Controlla che il file non sia corrotto
2. Verifica di aver inserito la password se necessaria
3. Prova con un file più piccolo

**Se hai problemi persistenti:**

- Contatta l'assistenza al numero: **{numero_telefonico}**

---

### File Originali

- I tuoi file originali **non vengono mai cancellati**
- Rimangono sempre nella cartella monitorata
- Puoi spostarli o cancellarli manualmente quando vuoi

---

## 💡 Consigli per un Uso Ottimale

### Organizzazione

- Crea una cartella dedicata solo per le conversioni
- Mantieni le tre cartelle (input, success, error) nella stessa directory principale
- Svuota periodicamente le cartelle per evitare accumuli

### Performance

- Evita di inserire troppi file molto grandi contemporaneamente
- Se hai molti file, convertili a gruppi
- Tieni attivo solo il monitoraggio quando serve

### Workflow Consigliato

1. **Prepara** i file da convertire in una cartella temporanea
2. **Configura** le opzioni di conversione se necessario
3. **Attiva** il monitoraggio
4. **Copia** i file nella cartella monitorata
5. **Controlla** il log per seguire il progresso
6. **Organizza** i file convertiti dalla cartella success

---

## 🆘 Risoluzione Problemi Rapida

| Problema                     | Soluzione                                                      |
| ---------------------------- | -------------------------------------------------------------- |
| Punto rosso nell'interfaccia | Clicca "Avvia Monitoraggio"                                    |
| File non viene rilevato      | Controlla che il monitoraggio sia attivo (punto verde)         |
| Conversione fallisce         | Verifica formato supportato e inserisci password se necessaria |
| App si blocca                | Riavvia l'applicazione                                         |
| Errori continui              | Riavvia il computer, poi l'app                                 |
| Non trovo i file convertiti  | Controlla la cartella "Success" (pulsante "File Convertiti")   |
| Popup non appare             | Attendi qualche secondo, l'app sta rilevando il file           |

---

## 📞 Supporto

Se hai problemi che non riesci a risolvere con questo manuale, contatta l'assistenza tecnica:

**Telefono**: {numero_telefonico}

**Prima di chiamare, prepara queste informazioni:**

- Sistema operativo (Windows, macOS, Linux)
- Tipo di file che stai cercando di convertire
- Messaggio di errore esatto (se presente)
- Cosa stavi facendo quando è apparso il problema

---

*Questo manuale copre tutte le funzionalità principali di File Converter Manager. L'applicazione è progettata per essere semplice e intuitiva - la maggior parte delle operazioni dovrebbe essere immediata dopo aver letto questa guida!*