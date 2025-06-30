# Documentazione Converter WebService

## Panoramica del Progetto

Il Converter WebService è un'applicazione distribuita che permette la conversione di file attraverso un'architettura client-server. Il sistema è composto da un'applicazione client che monitora una cartella locale e da un servizio web REST che gestisce le conversioni dei file.

### Architettura del Sistema

```
Client Application (JavaFX) ←→ WebService REST API (Spring Boot) ←→ Conversion Engine
```

Il progetto segue un'architettura a microservizi dove:
- **Client**: Interfaccia grafica per la selezione dei file e delle opzioni di conversione
- **WebService**: API REST per la gestione delle richieste di conversione
- **Engine**: Motore di conversione che gestisce la logica di trasformazione dei file

## Struttura del Progetto

```
file-webService.client.objects-project/
├── pom.xml
├── config/
│   └── config.json
├── src/main/java/
│   ├── webService.client.gui/              # Interfaccia utente client
│   │   ├── MainApp.java
│   │   ├── MainViewController.java
│   │   └── ConfigWindowController.java
│   ├── webService.client.objects/          # Logica client
│   │   ├── Engine.java
│   │   ├── ConverterConfig.java
│   │   ├── DirectoryWatcher.java
│   │   └── Log.java
│   ├── webService.server/                  # Componenti server
│   │   ├── WebServiceApplication.java
│   │   ├── ConverterWebServiceController.java
│   │   ├── EngineWebService.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── ErrorResponse.java
│   └── webService.server.converters/       # Convertitori specifici
│       └── Converter.java (interface)
├── src/main/resources/
│   ├── GraphicalMenu.fxml
│   ├── ConfigWindow.fxml
│   └── application.properties
└── temp/
    ├── uploads/
    └── outputs/
```

## Componenti Server

### 1. WebServiceApplication.java

Classe principale dell'applicazione Spring Boot che gestisce il ciclo di vita del servizio web.

**Funzionalità principali:**
- Avvio e arresto del servizio web
- Inizializzazione dei gestori di configurazione
- Controllo dello stato del servizio

**Metodi pubblici:**
```java
public static void startWebService()    // Avvia il servizio web
public static void stopWebService()     // Ferma il servizio web  
public static boolean isRunning()       // Verifica se il servizio è attivo
```

### 2. ConverterWebServiceController.java

Controller REST che espone le API per la conversione dei file. Gestisce le richieste HTTP e orchestra il processo di conversione.

**Endpoints disponibili:**

#### GET `/api/converter/status`
Restituisce lo stato del servizio web.

**Risposta:**
```json
{
  "status": "active"
}
```

#### GET `/api/converter/conversions/{extension}`
Ottiene le possibili conversioni per un formato specifico.

**Parametri:**
- `extension`: Estensione del file di partenza

**Risposta:**
```json
["pdf", "docx", "txt"]
```

#### POST `/api/converter/convert`
Effettua la conversione di un file.

**Parametri:**
- `file`: File da convertire (MultipartFile)
- `targetFormat`: Formato di destinazione
- `configFile`: File di configurazione per la conversione

**Risposta:** File convertito come array di byte con headers appropriati

**Processo di conversione:**
1. Creazione directory temporanea univoca
2. Upload del file di configurazione
3. Salvataggio del file di input nella directory temporanea
4. Chiamata al motore di conversione
5. Preparazione della risposta HTTP con file convertito
6. Pulizia dei file temporanei

### 3. EngineWebService.java

Motore di conversione che gestisce la logica di trasformazione dei file. Supporta conversioni singole e multiple (tramite archivi ZIP).

**Funzionalità principali:**

#### `getPossibleConversions(String extension)`
Restituisce i formati disponibili per la conversione da un formato specifico.

#### `conversione(String srcExt, String outExt, File srcFile)`
Esegue la conversione del file:
- Verifica se è richiesta una conversione multipla (file ZIP)
- Gestisce conversioni singole o multiple
- Utilizza il pattern Strategy per selezionare il convertitore appropriato

#### Conversione Multipla
Per file ZIP contiene più file dello stesso formato:
1. Estrazione dei file dall'archivo
2. Conversione di ogni file individualmente
3. Ricompressione dei file convertiti

#### Conversione Singola
Per file individuali:
1. Selezione del convertitore tramite reflection
2. Istanziazione dinamica del convertitore
3. Esecuzione della conversione
4. Pulizia dei file temporanei

### 4. GlobalExceptionHandler.java

Gestore globale delle eccezioni che fornisce risposte strutturate per tutti gli errori del sistema.

**Eccezioni gestite:**
- `IllegalExtensionException` (Codice: 1001)
- `FileMoveException` (Codice: 1002)
- `UnsupportedConversionException` (Codice: 1003)
- `IOException` (Codice: 1004)
- `BatchConversionException` (Codice: 1005)
- `ConversionException` (Codice: 1006)
- `FileCreationException` (Codice: 1007)
- `EmptyFileException` (Codice: 1008)
- `FormatsException` (Codice: 1009)
- `PasswordException` (Codice: 1010)
- `NullPointerException` (Codice: 1011)
- `Exception` generica (Codice: 9999)

**Formato risposta errore:**
```json
{
  "errorCode": 1001,
  "message": "Descrizione dell'errore",
  "stackTrace": "Prima riga dello stack trace"
}
```

### 5. ErrorResponse.java

Classe DTO per la rappresentazione strutturata degli errori nelle risposte JSON.

**Campi:**
- `errorCode`: Codice numerico identificativo dell'errore
- `message`: Messaggio descrittivo dell'errore
- `stackTrace`: Prima riga dello stack trace per il debugging

## Flusso di Conversione

### 1. Richiesta Formati Disponibili
```
Client → GET /api/converter/conversions/{extension} → Server
Server → Consulta configurazione → Restituisce lista formati
```

### 2. Conversione File
```
Client → POST /api/converter/convert → Server
Server → Crea directory temporanea
Server → Salva file di input e configurazione
Server → Chiama EngineWebService.conversione()
EngineWebService → Seleziona convertitore dinamicamente
EngineWebService → Esegue conversione
Server → Prepara risposta HTTP con file convertito
Server → Pulisce file temporanei
Server → Restituisce file al client
```

## Gestione File Temporanei

Il sistema utilizza directory temporanee univoche per ogni conversione:

1. **Creazione**: `Files.createTempDirectory("conversion-" + UUID.randomUUID() + "-")`
2. **Utilizzo**: Salvataggio file di input e configurazione
3. **Pulizia**: Eliminazione automatica di file e directory al termine della conversione

Questa strategia garantisce:
- Isolamento delle conversioni concorrenti
- Prevenzione di conflitti di nomi file
- Pulizia automatica in caso di errori

## Gestione degli Errori

Il sistema implementa una gestione strutturata degli errori su più livelli:

### 1. Validazione Input
- Controllo esistenza e validità dei file
- Verifica supporto formati
- Validazione parametri richiesti

### 2. Gestione Eccezioni di Conversione
- Eccezioni specifiche per ogni tipo di errore
- Logging dettagliato per debugging
- Rollback automatico in caso di fallimento

### 3. Risposte HTTP Strutturate
- Codici di errore standardizzati
- Messaggi descrittivi per l'utente
- Stack trace per il debugging (solo primo livello)

## Sicurezza e Performance

### Gestione Concorrenza
- Directory temporanee univoche per conversioni parallele
- Pulizia automatica dei file temporanei
- Gestione safe dell'eliminazione file

### Validazione File
- Controllo tipo MIME tramite libreria Apache Tika
- Validazione estensioni supportate
- Controllo esistenza e integrità file

### Logging
- Logging strutturato con Log4j2
- Tracciamento completo delle operazioni
- Livelli di log configurabili (TRACE, INFO, WARN, ERROR)

## Configurazione

Il sistema utilizza due tipi di configurazione:

### 1. Configurazione Server (`serverConfig.json`)
Definisce i convertitori disponibili e le mappature formato → classe:
```json
{
  "conversions": {
    "pdf": {
      "docx": "webService.server.converters.PdfToDocxConverter",
      "txt": "webService.server.converters.PdfToTxtConverter"
    }
  }
}
```

### 2. Configurazione Conversione (`conversionContext`)
Parametri specifici per ogni conversione (caricata dinamicamente dal client):
- Abilitazione conversioni multiple
- Parametri specifici del convertitore
- Opzioni di formattazione

## Estendibilità

### Aggiunta Nuovi Convertitori
1. Implementare l'interfaccia `Converter`
2. Aggiungere la mappatura in `serverConfig.json`
3. Il sistema caricherà dinamicamente la classe tramite reflection

### Aggiunta Nuovi Formati
1. Implementare il convertitore specifico
2. Aggiornare la configurazione dei formati supportati
3. Opzionale: aggiungere validazioni specifiche nel `GlobalExceptionHandler`

Questa architettura modulare permette l'estensione del sistema senza modifiche al codice esistente, seguendo il principio Open/Closed del SOLID design.