## Convertitore di File - Java 8 & Spring Boot

## Descrizione

Applicazione client-server in Java 8 con Spring Boot.  
Il sistema consente la **conversione automatica di file** tramite monitoraggio di una cartella locale e l'invio di richieste al server per la conversione.

## Funzionalità principali

- Monitoraggio automatico di una directory sul client
- Invio file da convertire al server tramite REST API
- Conversione lato server
- GUI lato client per gestione impostazioni
- Aggiornamento impostazioni tramite JSON condivisi
- Logging avanzato con barre di progresso
- Separazione dei file convertiti con successo da quelli falliti

## Architettura

- **Client**:
  - Directory Watcher
  - GUI JavaFX
  - Configurazione in file JSON
- **Server**:
  - Spring Boot REST API
  - Moduli per la configurazione dei file
  - Motore centrale che gestisce le conversioni

## Requisiti

- Java 1.8
- Maven 3.6+
- Spring Boot 2.x
- IDE: IntelliJ IDEA

## Conversioni supportate

##### PDF

- PDF → DOC  

- PDF → DOCX  

- PDF → JPG

##### JSON e CSV

- JSON ↔ XLS  

- JSON ↔ XLSX  

- JSON ↔ ODS  

- CSV → JSON

##### Email

- EML → PDF  

- MSG → PDF

##### Testo

- TXT → DOCX  

- TXT → PDF

##### Compressed files

- ZIP ↔ TAR.GZ

##### Conversioni immagini bidirezionali

- BMP ↔ GIF  

- BMP ↔ JPEG  

- BMP ↔ JPG  

- BMP ↔ PNG  

- BMP ↔ TIFF  

- BMP ↔ WEBP

- GIF ↔ JPEG  

- GIF ↔ JPG  

- GIF ↔ PNG  

- GIF ↔ TIFF  

- GIF ↔ WEBP

- JPEG ↔ JPG  

- JPEG ↔ PNG  

- JPEG ↔ TIFF  

- JPEG ↔ WEBP

- JPG ↔ PNG  

- JPG ↔ TIFF  

- JPG ↔ WEBP

- PNG ↔ TIFF  

- PNG ↔ WEBP

- TIFF ↔ WEBP

- PSD ↔ BMP  

- PSD ↔ GIF  

- PSD ↔ JPEG  

- PSD ↔ JPG 

- PSD ↔ PNG  

- PSD ↔ TIFF  

- PSD ↔ WEBP

- ICO ↔ PNG  

- ICO ↔ JPEG 

- ICO ↔ JPG  

- ICO ↔ ICNS

- ICNS ↔ PNG  

- ICNS ↔ JPEG  

- ICNS ↔ JPG  

- ICNS ↔ ICO

##### Conversioni immagini unidirezionali

- PAM → ICO  

- PAM → JPEG  

- PAM → JPG  PAM → PNG

- PBM → ICO  

- PBM → JPEG  

- PBM → JPG  

- PBM → PNG

- PGM → ICO  

- PGM → JPEG  

- PGM → JPG  

- PGM → PNG

- PNM → ICO  

- PNM → JPEG  

- PNM → JPG  

- PNM → PNG

- PPM → ICO  

- PPM → JPEG  

- PPM → JPG  

- PPM → PNG

- IFF → ICO  

- IFF → JPEG  

- IFF → JPG  

- IFF → PNG

- TGA → BMP  

- TGA → ICO  

- TGA → JPEG  

- TGA → JPG  

- TGA → PNG

- XWD → ICO  

- XWD → JPEG  

- XWD → JPG  

- XWD → PNG
