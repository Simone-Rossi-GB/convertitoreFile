Il suddetto documento è da considerare come documentazione effettiva dell'applicazione PantheraConverter realizzata dagli studenti Pletti Gianluca, Tagliani Daniel, Rossi Simone, Cicalese Anton e Ferrari Paolo nel periodo di stage dal 16/06/2025 al 04/07/2025 all'azienda Sisthema S.p.A di Brescia (Via Flero 28, BS).

Il progetto segue il seguente albero di percorsi che deve il più possibile rispettare il seguente:

=======================================================================

PantheraConverter/
├── pom.xml
├── src/
│   ├── main/
|   |   ├── logs/
│   │   ├── java/
│   │   │   ├── gui/                             
│   │   │   │   ├── MainApp.java                 
│   │   │   │   ├── MainViewController.java      
│   │   │   │   └── ConfigWindowController.java  
│   │   │   │
│   │   │   ├── converter/                       
|   |   |   |   ├── config/
│   |	|   |   |	└── config.json
│   │   │   │   ├── ConvertionException.java
│   │   │   │   ├── Engine.java                 
│   │   │   │   ├── ConverterConfig.java         
│   │   │   │   ├── DirectoryWatcher.java        
│   │   │   │   ├── Utility.java
│   │   │   │   └── Log.java                     
│   │   │   │
│   │   │   ├── Converters/          
│   │   │   │   ├── AbstractPDFConverter.java             
│   │   │   │   ├── Converter.java     (Interface)           
│   │   │   │   └── [altri converter...]          
│   │   │   │
│   │   │   └── webservice/                       
│   │   │       ├── WebServiceApplication.java    
│   │   │       ├── EngineWebService.java  
│   │   │       ├── controller/
│   │   │       │   └── ConverterWebServiceController.java  
│   │   │       └── client/
│   │   │           ├── ConverterWebServiceClient.java 
│   │   │           └── ConversionResult.java     
│   │   │
│   │   └── resources/
│   │       ├── GraphicalMenu.fxml               
│   │       ├── ConfigWindow.fxml                
│   │       └── application.properties            
│   │
│   └── temp/                                     
│       └── uploads/                              
│                                         
└── target/                                       # Build output Maven


=======================================================================

## 1. ConverterWebServiceController.java

**Scopo**: Controller REST Spring Boot che espone API per conversioni file via web service.

### Funzioni principali:

**`getStatus()`**

- **Endpoint**: GET `/api/converter/status`
- **Funzione**: Verifica che il web service sia attivo
- **Ritorna**: JSON con `{"status": "active"}`

**`getPossibleConversions(@PathVariable String extension)`**

- **Endpoint**: GET `/api/converter/conversions/{extension}`
- **Funzione**: Restituisce i formati di destinazione disponibili per una data estensione
- **Input**: Estensione file (es. "pdf")
- **Output**: Lista di estensioni supportate per la conversione

**`convertFile()`**

- **Endpoint**: POST `/api/converter/convert`
- **Funzione**: Converte un file caricato nel formato richiesto
- **Parametri**:
    - `file`: File da convertire (MultipartFile)
    - `targetFormat`: Formato di destinazione
    - `password`: Password per PDF protetti (opzionale)
    - `mergeImages`: Flag per unire pagine PDF in singola immagine (opzionale)

**Processo di conversione**:

1. Crea directory temporanea univoca con UUID
2. Salva il file caricato nella directory temporanea
3. Chiama `EngineWebService` per la conversione effettiva
4. Legge il file convertito e lo restituisce come array di byte
5. Pulisce automaticamente tutti i file temporanei nel `finally`

**Funzioni di utilità**:

- `getFileExtension()`: Estrae l'estensione da un nome file
- `determineMediaType()`: Determina il Content-Type HTTP corretto per ogni formato

---

## 2. EngineWebService.java

**Scopo**: Engine di conversione ottimizzato per l'uso in web service, gestisce conversioni senza spostare automaticamente i file.

### Funzioni principali:

**`setConfig()`**

- Carica la configurazione da `config.json`
- Inizializza l'oggetto `ConverterConfig`

**`getConfigAsJson()`**

- Restituisce il contenuto del file di configurazione come stringa JSON

**`getPossibleConversions(String extension)`**

- Consulta la configurazione per determinare i formati di destinazione disponibili
- Restituisce lista di estensioni supportate per la conversione

**Metodi di conversione multipli** (overloaded):

- `conversione(srcExt, outExt, srcFile, outputDirectory)`: Conversione base
- `conversione(srcExt, outExt, srcFile, password, outputDirectory)`: PDF protetti
- `conversione(srcExt, outExt, srcFile, union, outputDirectory)`: PDF con unione pagine
- `conversione(srcExt, outExt, srcFile, extraParam)`: Conversione con parametro extra

**`executeConversionWebService()` (metodo principale)**:

1. **Validazione**: Verifica parametri e supporto della conversione
2. **Setup temporaneo**: Crea directory temporanea per la conversione
3. **Preparazione file**: Copia e rinomina il file con suffisso univoco
4. **Conversione**: Istanzia e chiama il converter appropriato
5. **Ricerca output**: Cerca i file risultanti in multiple location (temp dir, success dir)
6. **Finalizzazione**: Sposta il file convertito nella directory di output specificata
7. **Pulizia**: Elimina ricorsivamente la directory temporanea

**Funzioni di supporto**:

- `deleteDirectoryRecursively()`: Elimina directory e tutto il contenuto
- `checkParameters()`: Valida parametri e determina il converter da usare
- `giveBackNewFileWithNewName()`: Genera nome file con suffisso
- `rinominaFile()`: Rinomina file con gestione errori

---

## 3. WebServiceApplication.java

**Scopo**: Classe principale Spring Boot per gestire il ciclo di vita del web service.

### Funzioni:

**`startWebService()`**

- Avvia il web service Spring Boot sulla porta 8080
- Verifica che non sia già attivo prima di avviarlo

**`stopWebService()`**

- Ferma il web service se attivo
- Chiude il context Spring

**`isRunning()`**

- Verifica se il web service è attualmente attivo

**`main()`**

- Entry point che avvia automaticamente il web service

---

## 4. ConverterWebServiceClient.java

**Scopo**: Client per comunicare con il web service da applicazioni esterne.

### Funzioni principali:

**`isServiceAvailable()`**

- Testa la connettività al web service
- Chiama l'endpoint `/status` e verifica la risposta
- Gestisce eccezioni di rete e timeout

**`getPossibleConversions(String extension)`**

- Richiede al web service i formati disponibili per una estensione
- Converte la risposta da array a `**List<String>`**
- Gestisce errori HTTP e di rete

**`convertFile()`**

- **Funzione principale del client**
- Invia file al web service per la conversione
- **Processo**:
    1. Verifica disponibilità servizio
    2. Prepara richiesta multipart con file e parametri
    3. Esegue POST al web service
    4. Riceve array di byte del file convertito
    5. Salva il file localmente nel percorso specificato
- **Gestione errori completa**: Errori HTTP, di rete, I/O
- **Ritorna**: `ConversionResult` con stato e messaggio

---

## 5. ConversionResult.java

**Scopo**: Classe per incapsulare il risultato di una conversione.

### Proprietà:

- `success`: Boolean che indica successo/fallimento
- `message`: Messaggio descrittivo per successi
- `error`: Messaggio di errore dettagliato

### Costruttori:

- `ConversionResult(success, message, error)`: Costruttore completo
- `ConversionResult(success, message)`: Costruttore semplificato (error = null)

### Metodi:

- `isSuccess()`, `getMessage()`, `getError()`: Getter standard
- `hasError()`: Verifica presenza di errori
- `getStatusMessage()`: Restituisce message o error in base al successo
- `success(String message)`: Factory method per successi
- `error(String error)`: Factory method per errori

---

## 6. MainViewController.java

**Scopo**: Controller principale dell'interfaccia JavaFX per gestire monitoraggio cartelle e conversioni.

### Variabili di stato:

- Contatori: `fileRicevuti`, `fileConvertiti`, `fileScartati`
- Percorsi cartelle: `monitoredFolderPath`, `convertedFolderPath`, `failedFolderPath`
- Flags: `isMonitoring`, `useWebService`, `monitorAtStart`
- Componenti: `engine`, `webServiceClient`, `watcherThread`

### Funzioni principali:

**`initialize()`**

- Inizializza engine e web service client
- Carica configurazione da JSON
- Setup event handlers per i controlli UI
- Avvia monitoraggio automatico se configurato

**`toggleMonitoring()`**

- Attiva/disattiva il monitoraggio della cartella
- Gestisce thread `DirectoryWatcher`
- Aggiorna UI e resetta contatori

**`launchDialogConversion(File srcFile)`**

- **Funzione chiamata quando viene rilevato un nuovo file**
- Determina formati disponibili (web service o engine locale)
- Mostra dialog per selezione formato
- Avvia conversione in thread separato

**`performConversionWithFallback()`**

- **Logica principale di conversione con fallback**
- **Processo**:
    1. Gestisce dialoghi per PDF (password, unione pagine)
    2. **Primo tentativo**: Web service se disponibile
    3. **Secondo tentativo**: Engine locale se web service fallisce
    4. Gestisce file originale dopo successo/fallimento
    5. Aggiorna contatori e UI

**Gestione dialoghi**:

- `launchDialogPdfSync()`: Password PDF (sincrono per threading)
- `launchDialogUnisciSync()`: Opzione unione pagine PDF
- `launchAlertSuccess()`, `launchAlertError()`: Notifiche risultato

**Funzioni di utilità**:

- `loadConfiguration()`: Carica settings da config.json
- `openConfigurationWindow()`: Apre editor configurazione
- `openFolder()`: Apre cartelle in file explorer
- `moveOriginalFileAfterSuccess()`, `moveFileToErrorFolder()`: Gestione file post-conversione
- `addLogMessage()`: Aggiunge messaggi al log UI
- `stampaRisultati()`: Aggiorna contatori nell'interfaccia

---

## 7. MainApp.java

**Scopo**: Classe principale JavaFX Application.

### Funzioni:

**`start(Stage primaryStage)`**

- Entry point JavaFX
- Configura finestra principale (titolo, dimensioni, non ridimensionabile)
- Carica la vista principale

**`loadMainView()`**

- Carica il file FXML `GraphicalMenu.fxml`
- Configura il controller e scene
- Gestisce errori di caricamento FXML con dialog dettagliati
- Setup handler per chiusura applicazione

**`showErrorDialog()`**

- Mostra errori critici di configurazione
- Termina l'applicazione in caso di errori FXML

---

## 8. ConfigWindowController.java

**Scopo**: Controller per la finestra di configurazione dell'applicazione.

### Funzioni principali:

**`initialize()`**

- Setup iniziale dei controlli UI
- Configura stili e validazione real-time per i campi directory
- Imposta TextArea conversioni come sola lettura

**`setEngine()` e `loadCurrentConfiguration()`**

- Riceve riferimenti engine e main controller
- Carica configurazione corrente nei campi UI
- Estrae e visualizza sezione "conversions" del JSON

**Gestione directory**:

- `browseMonitoredDirectory()`, `browseSuccessDirectory()`, `browseErrorDirectory()`: DirectoryChooser per selezione cartelle
- `validateDirectoryPath()`: Validazione real-time con feedback visivo (colori)
- `validateDirectories()`: Validazione completa e creazione directory mancanti

**`toggleMonitorAtStart()`**

- Cambia flag avvio automatico monitoraggio
- Aggiorna UI con colori e testo appropriati

**`saveConfiguration()`**

- **Processo di salvataggio**:
    1. Valida tutte le directory
    2. Ricostruisce JSON mantenendo sezione "conversions" originale
    3. Aggiorna solo campi modificabili
    4. Salva tramite engine con formattazione pretty-print
    5. Notifica successo e chiude finestra

**Funzioni di supporto**:

- `hasUnsavedChanges()`: Confronta valori correnti con configurazione salvata
- `cancelAndClose()`: Chiusura con controllo modifiche non salvate
- `updateStatus()`: Aggiorna label di stato con colori
- `showAlert()`: Utility per dialog standardizzati

---

Architettura Generale del Sistema:

1. **Layer Presentation**: JavaFX UI (MainApp, Controllers)
2. **Layer Business**: Engine di conversione locale
3. **Layer Web Service**: Spring Boot REST API
4. **Layer Client**: Web service client con fallback
5. **Layer configuration**: Gestione JSON configuration

Il sistema implementa un pattern di **fallback intelligente**: tenta prima il web service per performance migliori, poi ricade sull'engine locale per garantire affidabilità.