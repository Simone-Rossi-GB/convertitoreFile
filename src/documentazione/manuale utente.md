# Manuale Utente - File Converter Manager

## Primo Avvio

### Requisiti di Sistema

- Java 8 o superiore.
- Permessi di lettura/scrittura sulle cartelle che userai.

## Come Funziona l'Applicazione

File Converter Manager è un'applicazione che converte automaticamente i file da un formato all'altro. Funziona monitorando una cartella: quando inserisci un file, l'app lo rileva e ti chiede in che formato convertirlo.

### Il Processo di Conversione

1. **Inserisci** il file nella cartella monitorata.
2. **Scegli** il formato di destinazione dal popup.
3. **Attendi** che il server converta il file.
4. **Trova** il file convertito nella cartella "File Convertiti".

## Interfaccia Principale

### Indicatori di Stato

- **Bottone Verde** (in basso a destra): Monitoraggio attivo.
- **Bottone Rosso** (in alto a basso): Monitoraggio disattivato.
- **Log dell'Applicazione**: Mostra in tempo reale cosa sta facendo l'app.

### Statistiche

L'applicazione tiene traccia di:

- **File Rilevati**: Quanti file hai inserito.
- **Conversioni Riuscite**: Quanti file sono stati convertiti con successo.
- **Conversioni Fallite**: Quanti file hanno avuto problemi.

## Gestione delle Cartelle

L'applicazione usa tre cartelle principali:

### Cartella Monitorata (Input)

- Qui inserisci i file da convertire.
- L'app monitora costantemente questa cartella.
- **Accesso rapido**: Pulsante "Carica File".

### Cartella File Convertiti (Success)

- I file convertiti con successo finiscono qui.
- **Accesso rapido**: Pulsante "File Convertiti".

### Cartella Conversioni Fallite (Error)

- I file che non si riescono a convertire finiscono qui.
- **Accesso rapido**: Pulsante "Conversioni Fallite".

## Configurazione

### Configurazione Base (Pulsante "Config")

**Cartelle Personalizzate**

- Puoi cambiare il percorso di tutte e tre le cartelle.
- Usa i pulsanti "Sfoglia..." per selezionare cartelle diverse.

**Monitoraggio Automatico**

- Attiva/disattiva l'avvio automatico del monitoraggio.
- Se attivo, al prossimo avvio l'app inizierà subito a monitorare.

### Configurazione Conversioni (Pulsante "Conversion Config")

**Password per File Protetti**

- Se devi convertire PDF o documenti protetti da password inserisci la password qui prima di iniziare la conversione.
- **Privacy**: La password è usata solo per la conversione, mai salvata o condivisa.

**Unione Pagine PDF**

- Quando converti PDF in immagini.
- **OFF**: Ogni pagina diventa un'immagine separata.
- **ON**: Tutte le pagine diventano un'unica immagine lunga.

**Output Zippato**

- **OFF**: Ricevi il file convertito direttamente.
- **ON**: Il file convertito viene messo in un archivio zip.
- Utile per ridurre le dimensioni o organizzare meglio i file.

**Conversione multpla**

- Quando devi convertire una cartella compressa.

- **OFF**: Viene convertito il singolo file zip.

- **ON**: Vengono convertiti tutti i file contenuti nella cartella compressa.

## Come Convertire i File

1. **Attiva il monitoraggio** (pulsante "Avvia Monitoraggio" se il punto è rosso).
2. **Clicca "Carica File"** per aprire la cartella monitorata.
3. **Copia/sposta il tuo file** nella cartella.
4. **Scegli il formato** dal popup che appare.
5. **Attendi** - vedrai il progresso nel log.
6. **Trova il file convertito** nella cartella "Success".

### Formati Supportati

- L'app rileva automaticamente il formato del file originale.
- Mostra solo i formati di destinazione disponibili.

## Gestione Errori e Problemi

### Indicatori di Problemi

- **Popup di errore**: Spiega brevemente cosa è andato storto.
- **File nella cartella Error**: Conversioni fallite.

### Cause Comuni di Errore

- File corrotto o non leggibile.
- Formato non supportato per la conversione richiesta.
- File troppo grande.
- File protetto senza password corretta.
- Problemi di connessione al server.

### File Originali

- I tuoi file originali **non vengono mai cancellati**.
- Puoi trovarli nella cartella monitorata o in quella dedicata alle conversioni fallite.

## Consigli per un Uso Ottimale

### Organizzazione

- Crea una cartella dedicata solo per le conversioni.
- Mantieni le tre cartelle (input, success, error) nella stessa directory principale.
- Svuota periodicamente le cartelle per evitare accumuli.

### Flusso di utilizzo Consigliato

1. **Configura** le opzioni di conversione se necessario.
2. **Attiva** il monitoraggio.
3. **Copia** i file nella cartella monitorata.
4. **Configura** le opzioni di conversione se necessario.
5. **Controlla** il log per seguire il progresso.
6. **Organizza** i file convertiti dalla cartella success.