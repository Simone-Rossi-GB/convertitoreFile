# Config documentation - File di Configurazione

## Introduzione

Il sistema di gestione della configurazione è stato progettato per gestire parametri di configurazione tramite file JSON, supportando operazioni di lettura e scrittura lato client e di sola lettura lato server. 

La configurazione può essere modificata dall'utente tramite un'interfaccia grafica dedicata nel client.

Al momento della conversione al server viene inviato il file json con i parametri necessari alla conversione. Il file viene memorizzato in una cartella temporanea ed eliminato al termine dell'operazione.

## Architettura del Sistema

### Lato client

- **config.json**: Contiene le informazioni relative alla cartella monitorata, a quelle di destinazione e ad altre opzioni utili all'applicazione lato client. Può essere modificato tramite un'apposita finestra dalla GUI.

- **conversionContext.json**: Contiene le informazioni relative alle opzioni di conversione (es. password, watermark, compressione, ecc...). Può essere modificato tramite un'apposita finestra dalla GUI.

### Lato server

- **serverConfig.json**: Contiene le informazioni relative alle conversioni supportate e i percorsi delle rispettive classi da istanziare per effettuare la conversione. Non può essere modificato.

- **conversionContext.json**: Contiene le informazioni relative alle opzioni di conversione (es. password, watermark, compressione, ecc...). Ciascuno è associato ad una singola richiesta di conversione e viene memorizzato in una cartella temporanea. Al termine della conversione viene eliminato.

## Classi

### Data (Classe Base)

Classe astratta che fornisce l'infrastruttura comune per la gestione dei dati di configurazione:

**Caratteristiche principali:**

- Mantiene un riferimento thread-safe al file JSON corrente
- Gestisce una mappa condivisa (`configDataMap`) contenente i dati parsati
- Fornisce caching del nodo radice per evitare riletture multiple
- Include logging per tracciamento delle operazioni

**Metodi chiave:**

- `update(ConfigInstance configInstance)`: Aggiorna la configurazione caricando un nuovo file o ricaricando quello modificato
- `getJsonFile()`: Restituisce il file JSON attualmente attivo

### Instance

Rappresenta un'istanza validata di configurazione associata a un file JSON specifico.

**Funzionalità:**

- **Validazione automatica**: Verifica la presenza di tutti i campi obbligatori durante la creazione
- **Campi obbligatori validati**:
  - `successOutputDir`: Directory per conversioni riuscite
  - `errorOutputDir`: Directory per conversioni fallite
  - `monitoredDir`: Directory da monitorare
  - `monitorAtStart`: Flag per avvio automatico monitoraggio
  - `conversions`: Struttura delle conversioni supportate

### Reader

Fornisce accesso di sola lettura ai parametri di configurazione tramite metodi statici.

**Importante**: Le modifiche vengono lette solo dalla mappa in memoria.

### Writer

Permette la modifica dinamica dei parametri di configurazione in memoria.

**Importante**: Le modifiche vengono applicate solo alla mappa in memoria. Per la persistenza su disco è necessario utilizzare `JsonWriter`.

### InstanceReader

Lettore specializzato per operazioni su file specifici, mantenendo un proprio riferimento al nodo radice.

**Vantaggi:**

- Isolamento per file: ogni istanza gestisce un file diverso
- Caching ottimizzato per istanza
- Supporto per operazioni concorrenti su file diversi

**Metodi mirror**: Fornisce gli stessi metodi di `Reader` ma per l'istanza specifica.

### InstanceWriter

Writer specializzato per file specifici con persistenza automatica.

## Gestione degli Errori

Il sistema utilizza `JsonStructureException` per segnalare problemi di struttura o contenuto:

- **File malformato**: JSON non valido
- **Campi mancanti**: Assenza di campi obbligatori
- **Tipi incompatibili**: Valori con tipo diverso da quello atteso

## Flusso tipico di utilizzo

1. Creare un' `Instance`  e fare un update prima di utilizzare la configurazione, in modo da caricarla correttamente.
2. Utilizzare i metodi statici della classe `Reader`per leggere le informazioni dalla mappa condivisa.
3. Usare i metodi statici della classe `InstanceWriter` per modificare direttamente il file json.
4. Fare un update dell'`Instance` per aggiornare le informazioni nella mappa condivisa.
