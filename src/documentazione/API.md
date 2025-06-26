# API Documentation - Converter Web Service

## Introduzione

Questo servizio REST permette di interrogare lo stato del server, ottenere le possibili conversioni di file supportate, caricare file di configurazione e convertire file da un formato a un altro.

Base URL: `/api/converter`  
Cross-Origin abilitato per tutti i domini.

## Endpoints

### 1. Stato del servizio

**GET** `/status`

**Descrizione:**  
Restituisce lo stato attuale del servizio.

**Request:** 

nessun parametro

**Response 200:**

`{   "status": "active" }`

### 2. Possibili conversioni per estensione

**GET** `/conversions/{extension}`

**Descrizione:**  
Restituisce una lista di formati in cui è possibile convertire un file con una data estensione di partenza.

**Path Parameters:**

- `extension` (string): estensione del file di partenza (es. `pdf`, `jpg`)

**Response 200:**

es.`[   "doc",   "docx",   "jpg" ]`

**Response 400:**  
In caso di errore (es. estensione non supportata), restituisce una lista con il messaggio d'errore.

### 3. Conversione file

**POST** `/convert`

**Descrizione:**  
Converte un file caricato da un formato di origine al formato di destinazione specificato.

**Request:**

- Parametri multipart/form-data:
  
  - `file` (file): file da convertire
  
  - `targetFormat` (string): formato di destinazione (es. `pdf`, `docx`)

**Response 200:**

- File convertito come allegato

- Headers:
  
  - `Content-Type`: media type del file convertito
  
  - `Content-Disposition`: attachment; filename="nomefile"
  
  - `Content-Length`: lunghezza in byte

**Response 500:**

- In caso di errore di conversione, ritorna una lista con il messaggio d’errore.

### 4. Upload file di configurazione base

**POST** `/configUpload`

**Descrizione:**  
Carica un file di configurazione JSON che aggiorna la configurazione base del sistema.

**Request:**

- Parametro multipart/form-data:
  
  - `file` (file JSON)

**Response 200:**  
Messaggio di conferma con il path dove il file è stato salvato.

**Response 400 o 500:**  
Messaggi di errore se il file è vuoto o non può essere salvato.



### 5. Upload file di configurazione conversion context

**POST** `/conversionContextUpload`

**Descrizione:**  
Carica un file JSON di configurazione per il contesto di conversione, aggiornando le regole o i parametri di conversione.

**Request:**

- Parametro multipart/form-data:
  
  - `file` (file JSON)

**Response 200:**  
Messaggio di conferma con il path di salvataggio.

**Response 400 o 500:**  
Messaggi di errore in caso di problemi.

## Flusso tipico di utilizzo

1. Il client Rileva un file da convertire.

2. Il client chiama `GET /conversions/{extension}` per ottenere i formati disponibili per la conversione.

3. L'utente sceglie il formato di destinazione tra quelli proposti.

4. Il client carica il file da convertire tramite `POST /convert`, specificando il formato di destinazione.

5. Il client riceve il file convertito come risposta.
   
   
