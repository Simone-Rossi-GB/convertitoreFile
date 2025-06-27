# API Documentation - Converter Web Service

## Introduzione

Questo servizio REST permette di interrogare lo stato del server, ottenere le possibili conversioni di file supportate per un dato formato e convertire file da un formato ad un altro.

Base URL: `/api/converter`  
Cross-Origin abilitato per tutti i domini.

## Endpoints

### Stato del servizio

**GET** `/status`

**Descrizione:**  
Restituisce lo stato attuale del servizio.

**Request:** 

Nessun parametro

**Response 200:**

`{   "status": "active" }`

### Possibili conversioni per estensione

**GET** `/conversions/{extension}`

**Descrizione:**  
Restituisce una lista di formati in cui è possibile convertire un file con una data estensione di partenza.

**Path Parameters:**

- `extension` (string): estensione del file di partenza (es. `pdf`, `jpg`)

**Response 200:**

es.`[   "doc",   "docx",   "jpg" ]`

### Conversione file

**POST** `/convert`

**Descrizione:**  
Converte un file caricato da un formato di origine al formato di destinazione specificato, applicando le opzioni impostate nel file conversionConfig.json passato dal client.

**Request:**

- Parametri multipart/form-data:
  
  - `file` (file): File da convertire
  
  - `targetFormat` (string): Formato di destinazione (es. `pdf`, `docx`)
  
  - `configFile` (file): File JSON con i parametri utili alla conversione

**Response 200:**

- File convertito come allegato

- Headers:
  
  - `Content-Type`: Media type del file convertito
  
  - `Content-Disposition`: Attachment; filename="nomefile"
  
  - `Content-Length`: Lunghezza in byte

## Errori

Gli errori vengono gestiti centralmente tramite un **Rest Controller Advice** che intercetta le eccezioni e ritorna una risposta con codice di errore mappato sulla base del tipo di eccezione.

##### Response 500 – Errore Interno del Server

Tutti gli errori restituiscono lo status HTTP **500 Internal Server Error** con corpo JSON nel seguente formato:
{
  "errorCode": <codice numerico>,
  "message": "Messaggio dell'eccezione",
  "stackTrace": "Prima riga dello stack trace"
}

##### Elenco dei codici di errore

###### **1001.**   IllegalExtensionException:

            Estensione del file non supportata o vietata.

###### **1002.**   FileMoveException:

            Errore durante lo spostamento o salvataggio del file.

###### **1003.**   UnsupportedConversionException:

            La conversione richiesta non è supportata.

###### **1004.**   IOException:

            Errore generico di input/output durante la conversione.

###### **1005**.   BatchConversionException:

            Errore durante l'elaborazione di un lotto di file.

###### **1006.**   ConversionException:

            Errore generico durante il processo di conversione.

###### **1007.**   FileCreationException:

            Impossibile creare il file convertito.

###### **1008.**   EmptyFileException:

            Il file in input è vuoto.

###### **1009.**   FormatsException:

            Errore nei formati di input/output specificati.

###### **1010.**   PasswordException:

           Il file è protetto da password o non accessibile.

###### **1011.**   NullPointerException:

            Oggetto null.

###### **9999.**   Exception:

            Errore interno inatteso (variabile nulla o oggetto non inizializzato).

## Flusso tipico di utilizzo

1. Il client fa una chiamata chiama `GET /conversions/{extension}` per ottenere i formati disponibili per la conversione.

2. Il server cerca nel file serverConfig.json le informazioni richieste e allega alla risposta la lista di estensioni per la conversione.

3. Il client fa una chiamata `POST /convert`, passando il file da convertire, l'estensione di destinazione e il file conversionContext.json, con i parametri validi solo per quella conversione.

4. Il server invia una risposta con un'array di byte che rappresenta il contenuto del file convertito e il mediaType rilevato nell'header
