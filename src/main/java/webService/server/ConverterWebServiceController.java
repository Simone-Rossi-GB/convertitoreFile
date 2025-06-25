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
     * @return lo stato del web service
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        logger.info("WebService: Richiesta stato ricevuta");
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
            return ResponseEntity.ok(conversions);
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat) {

        Path tempInputFilePath = null;
        Path conversionTempDir = null;
        File convertedOutputFile = null;

        try {
            logger.info("Inizio conversione file: {} -> {}", file.getOriginalFilename(), targetFormat);

            // Crea una directory temporanea univoca per questa conversione
            conversionTempDir = Files.createTempDirectory("conversion-" + UUID.randomUUID() + "-");
            logger.info("Creata directory temporanea: {}", conversionTempDir);

            // Salva il file caricato nella directory temporanea
            String originalFilename = file.getOriginalFilename();
            String extension = null;
            try {
                extension = Utility.getExtension(new File(Objects.requireNonNull(file.getOriginalFilename())));
            } catch (IllegalExtensionException e) {
                logger.error(e.getMessage());
            }

            assert originalFilename != null;
            tempInputFilePath = conversionTempDir.resolve(originalFilename);
            file.transferTo(tempInputFilePath);
            logger.info("File salvato in: {}", tempInputFilePath);

            // Chiama EngineWebService per la conversione
            File inputFileForEngine = tempInputFilePath.toFile();

            convertedOutputFile = engine.conversione(extension, targetFormat, inputFileForEngine);

            // Verifica che il file convertito esista
            if (convertedOutputFile == null || !convertedOutputFile.exists()) {
                return msgErrore("Il file convertito non è stato generato correttamente");
            }

            // Leggi i byte del file convertito
            byte[] fileBytes = Files.readAllBytes(convertedOutputFile.toPath());
            logger.info("WebService: File convertito letto, dimensione: {} bytes", fileBytes.length);

            // Determina il Content-Type corretto per la risposta HTTP
            MediaType contentType = determineMediaType(convertedOutputFile);
            // Costruisci la risposta con i byte del file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", convertedOutputFile.getName());
            headers.setContentLength(fileBytes.length);

            logger.info("Conversione completata con successo per: {}", originalFilename);
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (IllegalExtensionException | FileMoveException | UnsupportedConversionException e) {
            return msgErrore(e);
        }catch(IOException e){
            return msgErrore("Errore nella lettura del file convertito");
        } finally {
            //Pulisce i file temporanei e la directory temporanea
            try {
                if (tempInputFilePath != null && Files.exists(tempInputFilePath)) {
                    Files.delete(tempInputFilePath);
                    logger.info("File temporaneo di input eliminato");
                }
                if (convertedOutputFile != null && Files.exists(convertedOutputFile.toPath())) {
                    Files.delete(convertedOutputFile.toPath());
                    logger.info("File temporaneo di output eliminato");
                }
                // Elimina la directory temporanea se vuota
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
     * Carica il file di configurazione json inviato dal client
     * @param file file di configurazione
     * @return
     */
    @PostMapping("/configUpload")
    public ResponseEntity<String> configUpload(@RequestParam("file") MultipartFile file) {
        ResponseEntity<String> response = configFilesUpload(file);
        ConfigInstance ci = new ConfigInstance(new File(uploadDir, Objects.requireNonNull(file.getOriginalFilename())));
        ConfigData.update(ci);
        return response;
    }

    @PostMapping("/conversionContextUpload")
    public ResponseEntity<String> conversionContextUpload(@RequestParam("file") MultipartFile file) {
        ResponseEntity<String> response = configFilesUpload(file);
        logger.info(new File(uploadDir, Objects.requireNonNull(file.getOriginalFilename())).getAbsolutePath());
        ConversionContextInstance ci = new ConversionContextInstance(new File(uploadDir, Objects.requireNonNull(file.getOriginalFilename())));
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

    private ResponseEntity<Map<String, Object>> msgErrore(Exception e){
        return msgErrore(e.getMessage());
    }

    private ResponseEntity<Map<String, Object>> msgErrore(String msg){
        logger.error(msg);
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", msg);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Metodo che riconosce automaticamente il MediaType dal nome del file
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