package WebService.controller;

import WebService.EngineWebService;
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
    List<String> formatiImmagini = Arrays.asList("png", "tiff", "gif", "webp", "psd", "icns", "ico", "tga", "iff", "jpeg", "bmp", "jpg", "pnm", "pgm", "pgm", "ppm", "xwd");

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        Log.addMessage("WebService: Richiesta stato ricevuta");
        return ResponseEntity.ok(Collections.singletonMap("status", "active"));
    }

    @GetMapping("/conversions/{extension}")
    public ResponseEntity<List<String>> getPossibleConversions(@PathVariable String extension) {
        try {
            Log.addMessage("WebService: Richiesta conversioni possibili per estensione: " + extension);
            List<String> conversions = engineWebService.getPossibleConversions(extension);
            return ResponseEntity.ok(conversions);
        } catch (Exception e) {
            Log.addMessage("ERRORE WebService: Impossibile ottenere conversioni per " + extension + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "mergeImages", required = false, defaultValue = "false") boolean mergeImages) {

        Path tempInputFilePath = null;
        Path conversionTempDir = null;
        File convertedOutputFile = null;

        try {
            Log.addMessage("WebService: Inizio conversione file: " + file.getOriginalFilename() + " -> " + targetFormat);

            // 1. Crea una directory temporanea univoca per questa conversione
            conversionTempDir = Files.createTempDirectory("conversion-" + UUID.randomUUID().toString() + "-");
            Log.addMessage("WebService: Creata directory temporanea: " + conversionTempDir);

            // 2. Salva il file caricato nella directory temporanea
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            tempInputFilePath = conversionTempDir.resolve(originalFilename);
            file.transferTo(tempInputFilePath);
            Log.addMessage("WebService: File salvato in: " + tempInputFilePath);

            // 3. Chiama EngineWebService per la conversione
            File inputFileForEngine = tempInputFilePath.toFile();
            File outputDirectoryForEngine = conversionTempDir.toFile();

            // Chiamata ai metodi di EngineWebService che restituiscono il file convertito
            convertedOutputFile = engineWebService.conversione(extension, targetFormat, inputFileForEngine, outputDirectoryForEngine);


            // 4. Verifica che il file convertito esista
            if (convertedOutputFile == null || !convertedOutputFile.exists()) {
                throw new Exception("Il file convertito non è stato generato correttamente");
            }

            // 5. Leggi i byte del file convertito
            byte[] fileBytes = Files.readAllBytes(convertedOutputFile.toPath());
            Log.addMessage("WebService: File convertito letto, dimensione: " + fileBytes.length + " bytes");

            // 6. Determina il Content-Type corretto per la risposta HTTP
            MediaType contentType = determineMediaType(targetFormat);

            // 7. Costruisci la risposta con i byte del file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            headers.setContentDispositionFormData("attachment", convertedOutputFile.getName());
            headers.setContentLength(fileBytes.length);

            Log.addMessage("WebService: Conversione completata con successo per: " + originalFilename);
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            Log.addMessage("ERRORE WebService: Errore durante conversione: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Errore durante la conversione: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            // 8. Pulisci i file temporanei e la directory temporanea
            try {
                if (tempInputFilePath != null && Files.exists(tempInputFilePath)) {
                    Files.delete(tempInputFilePath);
                    Log.addMessage("WebService: File temporaneo di input eliminato");
                }
                if (convertedOutputFile != null && Files.exists(convertedOutputFile.toPath())) {
                    Files.delete(convertedOutputFile.toPath());
                    Log.addMessage("WebService: File temporaneo di output eliminato");
                }
                // Elimina la directory temporanea se vuota
                if (conversionTempDir != null && Files.exists(conversionTempDir)) {
                    Files.delete(conversionTempDir);
                    Log.addMessage("WebService: Directory temporanea eliminata");
                }
            } catch (IOException cleanupException) {
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