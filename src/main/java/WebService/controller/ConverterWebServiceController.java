package WebService.controller;

import WebService.EngineWebService;
import WebService.EngineWebService.ConversionInfo;
import WebService.EngineWebService.PasswordRequiredException;
import converter.Log;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/converter")
@CrossOrigin(origins = "*")
public class ConverterWebServiceController {

    private final EngineWebService engineWebService = new EngineWebService();
    private static final Logger logger = LogManager.getLogger(ConverterWebServiceController.class);

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        Log.addMessage("WebService: Richiesta stato ricevuta");
        logger.info("WebService: Richiesta stato ricevuta");
        return ResponseEntity.ok(Collections.singletonMap("status", "active"));
    }

    @GetMapping("/conversions/{extension}")
    public ResponseEntity<List<String>> getPossibleConversions(@PathVariable String extension) {
        try {
            Log.addMessage("WebService: Richiesta conversioni possibili per estensione: " + extension);
            logger.info("WebService: Richiesta conversioni possibili per estensione: {}", extension);
            List<String> conversions = engineWebService.getPossibleConversions(extension);
            return ResponseEntity.ok(conversions);
        } catch (Exception e) {
            logger.error("ERRORE WebService: Impossibile ottenere conversioni per {}: {}", extension, e.getMessage());
            Log.addMessage("ERRORE WebService: Impossibile ottenere conversioni per " + extension + ": " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Endpoint per verificare i requisiti di conversione di un file
     */
    @PostMapping("/check-requirements")
    public ResponseEntity<?> checkConversionRequirements(
            @RequestParam("file") MultipartFile file) {

        Path tempFilePath = null;
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);

            // Salva temporaneamente il file per il controllo
            tempFilePath = Files.createTempFile("check_", "_" + originalFilename);
            file.transferTo(tempFilePath);

            ConversionInfo info = engineWebService.checkConversionRequirements(extension, tempFilePath.toFile());

            Map<String, Object> response = new HashMap<>();
            response.put("canConvertWithoutParams", info.canConvertWithoutParams);
            response.put("requiresPassword", info.requiresPassword);
            response.put("supportsBooleanOption", info.supportsBooleanOption);
            response.put("passwordDescription", info.passwordDescription);
            response.put("booleanDescription", info.booleanDescription);

            logger.info("WebService: Controllo requisiti completato per: {}", originalFilename);
            Log.addMessage("WebService: Controllo requisiti completato per: " + originalFilename);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("WebService: Errore nel controllo requisiti: {}", e.getMessage());
            Log.addMessage("ERRORE WebService: Errore nel controllo requisiti: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Errore nel controllo requisiti: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

        } finally {
            if (tempFilePath != null) {
                try {
                    Files.deleteIfExists(tempFilePath);
                } catch (IOException e) {
                    logger.warn("WebService: Impossibile eliminare file temporaneo: {}", e.getMessage());
                }
            }
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "booleanOption", required = false, defaultValue = "false") boolean booleanOption) {

        Path tempInputFilePath = null;
        Path conversionTempDir = null;
        File convertedOutputFile = null;

        try {
            logger.info("WebService: Inizio conversione file: {} -> {}", file.getOriginalFilename(), targetFormat);
            Log.addMessage("WebService: Inizio conversione file: " + file.getOriginalFilename() + " -> " + targetFormat);

            // 1. Crea una directory temporanea univoca per questa conversione
            conversionTempDir = Files.createTempDirectory("conversion-" + UUID.randomUUID().toString() + "-");
            logger.info("WebService: Creata directory temporanea: {}", conversionTempDir);
            Log.addMessage("WebService: Creata directory temporanea: " + conversionTempDir);

            // 2. Salva il file caricato nella directory temporanea
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            tempInputFilePath = conversionTempDir.resolve(originalFilename);
            file.transferTo(tempInputFilePath);
            logger.info("WebService: File salvato in: {}", tempInputFilePath);
            Log.addMessage("WebService: File salvato in: " + tempInputFilePath);

            // 3. Verifica automaticamente i requisiti del file
            File inputFileForEngine = tempInputFilePath.toFile();
            ConversionInfo conversionInfo = engineWebService.checkConversionRequirements(extension, inputFileForEngine);

            // 4. Se richiede password ma non è stata fornita, restituisci errore specifico
            if (conversionInfo.requiresPassword && (password == null || password.isEmpty())) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Il file richiede una password");
                errorResponse.put("errorCode", "PASSWORD_REQUIRED");
                errorResponse.put("passwordDescription", conversionInfo.passwordDescription);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 5. Chiama EngineWebService per la conversione con i parametri appropriati
            File outputDirectoryForEngine = conversionTempDir.toFile();

            try {
                convertedOutputFile = engineWebService.conversione(
                        extension,
                        targetFormat,
                        inputFileForEngine,
                        outputDirectoryForEngine,
                        password,
                        booleanOption
                );
            } catch (PasswordRequiredException e) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", e.getMessage());
                errorResponse.put("errorCode", "PASSWORD_REQUIRED");
                errorResponse.put("passwordDescription", conversionInfo.passwordDescription);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 6. Verifica che il file convertito esista
            if (convertedOutputFile == null || !convertedOutputFile.exists()) {
                throw new Exception("Il file convertito non è stato generato correttamente");
            }

            // 7. Leggi i byte del file convertito
            byte[] fileBytes = Files.readAllBytes(convertedOutputFile.toPath());
            logger.info("WebService: File convertito letto, dimensione: {} bytes", fileBytes.length);
            Log.addMessage("WebService: File convertito letto, dimensione: " + fileBytes.length + " bytes");

            // 8. Determina il Content-Type corretto per la risposta HTTP
            MediaType contentType = determineMediaType(targetFormat);

            // 9. Costruisci la risposta con i byte del file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", convertedOutputFile.getName());
            headers.setContentLength(fileBytes.length);

            logger.info("WebService: Conversione completata con successo per: {}", originalFilename);
            Log.addMessage("WebService: Conversione completata con successo per: " + originalFilename);
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("WebService: Errore durante conversione: {}", e.getMessage());
            Log.addMessage("ERRORE WebService: Errore durante conversione: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Errore durante la conversione: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);

        } finally {
            // 10. Pulisci i file temporanei e la directory temporanea
            try {
                if (tempInputFilePath != null && Files.exists(tempInputFilePath)) {
                    Files.delete(tempInputFilePath);
                    logger.info("WebService: File temporaneo di input eliminato");
                    Log.addMessage("WebService: File temporaneo di input eliminato");
                }
                if (convertedOutputFile != null && Files.exists(convertedOutputFile.toPath())) {
                    Files.delete(convertedOutputFile.toPath());
                    logger.info("WebService: File temporaneo di output eliminato");
                    Log.addMessage("WebService: File temporaneo di output eliminato");
                }
                // Elimina la directory temporanea se vuota
                if (conversionTempDir != null && Files.exists(conversionTempDir)) {
                    Files.delete(conversionTempDir);
                    logger.info("WebService: Directory temporanea eliminata");
                    Log.addMessage("WebService: Directory temporanea eliminata");
                }
            } catch (IOException cleanupException) {
                logger.error("WebService: Errore durante la pulizia dei file temporanei: {}", cleanupException.getMessage());
                Log.addMessage("ERRORE WebService: Errore durante la pulizia dei file temporanei: " + cleanupException.getMessage());
                cleanupException.printStackTrace();
            }
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private MediaType determineMediaType(String targetFormat) {
        switch (targetFormat.toLowerCase()) {
            case "pdf": return MediaType.APPLICATION_PDF;
            case "jpg":
            case "jpeg": return MediaType.IMAGE_JPEG;
            case "png": return MediaType.IMAGE_PNG;
            case "gif": return MediaType.IMAGE_GIF;
            case "bmp": return MediaType.valueOf("image/bmp");
            case "tiff": return MediaType.valueOf("image/tiff");
            case "webp": return MediaType.valueOf("image/webp");
            case "zip": return MediaType.valueOf("application/zip");
            case "json": return MediaType.APPLICATION_JSON;
            case "xml": return MediaType.APPLICATION_XML;
            case "txt": return MediaType.TEXT_PLAIN;
            case "html": return MediaType.TEXT_HTML;
            case "doc":
            case "docx": return MediaType.valueOf("application/msword");
            case "xls":
            case "xlsx": return MediaType.valueOf("application/vnd.ms-excel");
            case "ppt":
            case "pptx": return MediaType.valueOf("application/vnd.ms-powerpoint");
            default: return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}