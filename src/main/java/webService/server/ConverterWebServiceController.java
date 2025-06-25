package webService.server;

import converters.exception.FileMoveException;
import converters.exception.IllegalExtensionException;
import converters.exception.UnsupportedConversionException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import webService.Utility;
import org.apache.tika.Tika;
import webService.configuration.configHandlers.config.ConfigData;
import webService.configuration.configHandlers.config.ConfigInstance;
import webService.configuration.configHandlers.conversionContext.ConversionContextData;
import webService.configuration.configHandlers.conversionContext.ConversionContextInstance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


@RestController
@RequestMapping("/api/converter")
@CrossOrigin(origins = "*")
public class ConverterWebServiceController {
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;
    private static final Tika tika = new Tika();
    EngineWebService engine = new EngineWebService();
    private static final Logger logger = LogManager.getLogger(ConverterWebServiceController.class);

    /**
     * Ogni risposta al client viene data tramite json, più precisamente con un oggetto della
     * classe ResponseEntity
     */

    /**
     * @return lo stato del web service
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        logger.info("WebService: Richiesta stato ricevuta");
        //ritorno una mappa immutabile con una sola chiave e un solo valore
        return ResponseEntity.ok(Collections.singletonMap("status", "active"));
    }

    /**
     * ritorna le possibili conversioni in base al file .json
     * @param extension estensione di partenza
     * @return
     */
    @GetMapping("/conversions/{extension}")
    public ResponseEntity<List<String>> getPossibleConversions(@PathVariable String extension) {
        try {
            logger.info("Richiesta conversioni possibili per estensione: {}", extension);
            List<String> conversions = engine.getPossibleConversions(extension);
            // ritorno un json contenente una lista con le possibili conversioni da un determinato
            // formato d'origine
            return ResponseEntity.ok(conversions);
        } catch (Exception e) {
            logger.error(e.getMessage());
            //ritorno una lista immutabile con un solo elemento ovvero l'eccezione generata.
            return ResponseEntity.badRequest()
                    .body(Collections.singletonList(e.getMessage()));
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertFile(
            @RequestParam("file") MultipartFile file, //parametri richiesti dopo il /convert/
            @RequestParam("targetFormat") String targetFormat) {

        Path tempInputFilePath = null;
        Path conversionTempDir = null;
        File convertedOutputFile = null;

        try {
            logger.info("Inizio conversione file: {} -> {}", file.getOriginalFilename(), targetFormat);

            // Crea una directory temporanea con identificativo univoco per questa conversione
            conversionTempDir = Files.createTempDirectory("conversion-" + UUID.randomUUID() + "-");
            logger.info("Creata directory temporanea: {}", conversionTempDir);

            // Salva il file caricato nella directory temporanea
            String originalFilename = file.getOriginalFilename();
            String extension = null;

            try {
                // otteniamo direttamente dal file ricevuto il formato d'origine
                extension = Utility.getExtension(new File(Objects.requireNonNull(file.getOriginalFilename())));
            } catch (IllegalExtensionException e) {
                logger.error(e.getMessage());
                // gestiamo l'eccezione ritornando un InternalServerError
                return ResponseEntity.internalServerError()
                        .body(Collections.singletonList(e.getMessage()));
            }

            assert originalFilename != null; // diciamo al JVM che il nome non è mai null

            // prendiamo la directory temporanea e col metodo resolve mettiamo in
            // tempInputFilePath  percorsoTemporaneo/originalFileName.estensione
            tempInputFilePath = conversionTempDir.resolve(originalFilename);

            file.transferTo(tempInputFilePath); // spostiamo il file nella directory temporanea
            logger.info("File salvato in: {}", tempInputFilePath);

            // creiamo un oggetto file dal percorso del file
            File inputFileForEngine = tempInputFilePath.toFile();

            // Chiama EngineWebService per la conversione
            convertedOutputFile = engine.conversione(extension, targetFormat, inputFileForEngine);

            // Verifica che il file convertito esista
            if (convertedOutputFile == null || !convertedOutputFile.exists()) {
                return ResponseEntity.internalServerError()
                        .body(Collections.singletonList("Il file convertito non è stato generato correttamente"));
            }

            // crea un array di byte per la risposta al client leggendo i byte del file convertito
            byte[] fileBytes = Files.readAllBytes(convertedOutputFile.toPath());
            logger.info("WebService: File convertito letto, dimensione: {} bytes", fileBytes.length);

            // Determina il Content-Type corretto per la risposta HTTP
            MediaType contentType = determineMediaType(convertedOutputFile);

            // Costruisci la risposta con i byte del file
            HttpHeaders headers = new HttpHeaders();

            // inseriamo nell'header il tipo di contenuto della risposta
            headers.setContentType(contentType);

            // aggiungiamo all'header come allegato il nome del file convertito
            headers.setContentDispositionFormData("attachment", convertedOutputFile.getName());

            // aggiungiamo all'header la lunghezza in byte del contenuto della risposta
            headers.setContentLength(fileBytes.length);

            logger.info("Conversione completata con successo per: {}", originalFilename);
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (IllegalExtensionException | FileMoveException | UnsupportedConversionException e) {
            return ResponseEntity.internalServerError() // errore con la conversione
                    .body(Collections.singletonList(e.getMessage()));
        }catch(IOException e){
            return ResponseEntity.internalServerError() // errore nella lettura del file
                    .body(Collections.singletonList(e.getMessage()));
        } finally {
            try {
                if (tempInputFilePath != null && Files.exists(tempInputFilePath)) {
                    // elimina il file originale temporaneo spostato nella directory univoca
                    Files.delete(tempInputFilePath);
                    logger.info("File temporaneo di input eliminato");
                }
                if (convertedOutputFile != null && Files.exists(convertedOutputFile.toPath())) {
                    // elimina il file convertito temporaneo
                    Files.delete(convertedOutputFile.toPath());
                    logger.info("File temporaneo di output eliminato");
                }

                // Elimina la directory univoca temporanea se vuota
                if (conversionTempDir != null && Files.exists(conversionTempDir)) {
                    //Elimina eventuali file temporanei creati nella cartella
                    eliminaContenuto(conversionTempDir.toFile());
                    logger.info("Directory temporanea svuotata");
                    //Elimina la cartella
                    Files.delete(conversionTempDir);
                    logger.info("Directory temporanea eliminata");
                }
            } catch (IOException cleanupException) {
                logger.error("Errore durante la pulizia dei file temporanei: " + cleanupException.getMessage());
            }
        }
    }

    /**
     * Chiamata POST al server per
     * caricare il file di configurazione json inviato dal client
     * @param file file di configurazione
     * @return
     */
    @PostMapping("/configUpload")
    public ResponseEntity<String> configUpload(@RequestParam("file") MultipartFile file) {
        ResponseEntity<String> response = configFilesUpload(file);

        // creiamo una nuova istanza di configurazione usando il file ricevuto
        ConfigInstance ci = new ConfigInstance(new File(uploadDir, Objects.requireNonNull(file.getOriginalFilename())));

        // aggiorniamo la configurazione
        ConfigData.update(ci);

        return response;
    }

    /**
     * Chiamata POST al server per
     * caricare il file di contesto json per la conversione inviato dal client
     * @param file file di contesto della conversione
     * @return
     */
    @PostMapping("/conversionContextUpload")
    public ResponseEntity<String> conversionContextUpload(@RequestParam("file") MultipartFile file) {
        ResponseEntity<String> response = configFilesUpload(file);

        // ricarica la mappa di configurazione conversion context
        logger.info(new File(uploadDir, Objects.requireNonNull(file.getOriginalFilename())).getAbsolutePath());

        // creiamo una nuova istanza di contesto per la conversione usando il file ricevuto
        ConversionContextInstance ci = new ConversionContextInstance(new File(uploadDir, Objects.requireNonNull(file.getOriginalFilename())));

        // aggiorniamo il contesto
        ConversionContextData.update(ci);
        return response;
    }


    private ResponseEntity<String> configFilesUpload (MultipartFile file){
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File vuoto.");
        }

        try {
            // Percorso relativo alla directory di lavoro dell'app
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            file.transferTo(filePath);

            logger.info("Working directory: {}", System.getProperty("user.dir"));
            logger.info("File {} salvato in {}", file.getOriginalFilename(), filePath.toAbsolutePath());
            return ResponseEntity.ok("File salvato: " + filePath.toAbsolutePath());

        } catch (IOException e) {
            logger.error("Errore nel caricamento del file: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Errore nel caricamento del file di configurazione");
        }
    }

    /**
     * Elimina il contenuto di una cartella in maniera ricorsiva
     * @param directory cartella di cui eliminare il contenuto
     */
    private void eliminaContenuto(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    eliminaContenuto(file); // Ricorsione
                }
                file.delete(); // Elimina file o cartella vuota
            }
        }
    }

    /**
     * Metodo che riconosce automaticamente il MediaType dal nome del file tramite libreria Tika
     * @param file fileConvertito
     * @return mediaType corretto
     * @throws IllegalExtensionException mediatype non riconosciuto
     */
    private MediaType determineMediaType(File file) {
        try {
            String mimeType = tika.detect(file);
            return MediaType.parseMediaType(mimeType);
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}