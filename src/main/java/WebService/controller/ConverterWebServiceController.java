package WebService.controller;

import converter.Engine; // Assicurati che l'import sia corretto
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus; // Import HttpStatus

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;

@RestController
@RequestMapping("/api/converter")
@CrossOrigin(origins = "*")
public class ConverterWebServiceController {

    private final Engine engine = new Engine();

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Collections.singletonMap("status", "active"));
    }

    @GetMapping("/conversions/{extension}")
    public ResponseEntity<List<String>> getPossibleConversions(@PathVariable String extension) {
        try {
            List<String> conversions = engine.getPossibleConversions(extension);
            return ResponseEntity.ok(conversions);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<?> convertFile( // Changed return type to ResponseEntity<?>
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam("targetFormat") String targetFormat,
                                          @RequestParam(value = "password", required = false) String password,
                                          @RequestParam(value = "mergeImages", required = false, defaultValue = "false") boolean mergeImages) {

        Path tempInputFilePath = null; // Il file originale caricato
        Path conversionTempDir = null; // La directory temporanea per questa conversione
        Path convertedOutputFilePath = null; // Il file convertito

        try {
            // 1. Crea una directory temporanea univoca per questa singola conversione
            conversionTempDir = Files.createTempDirectory("conversion-" + UUID.randomUUID().toString() + "-");

            // 2. Salva il file caricato nella directory temporanea
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            tempInputFilePath = conversionTempDir.resolve(originalFilename);
            file.transferTo(tempInputFilePath);

            // 3. Chiama l'Engine per la conversione, passando la directory temporanea come output
            File inputFileForEngine = tempInputFilePath.toFile();
            File outputDirectoryForEngine = conversionTempDir.toFile(); // Passiamo la stessa directory come output

            File convertedFileFromEngine;

            // Chiamata ai NUOVI metodi `conversione` di Engine che restituiscono il file
            if (password != null && !password.trim().isEmpty()) {
                if (mergeImages && targetFormat.equals("jpg")) { // Controlla la logica per mergeImages e jpg
                    convertedFileFromEngine = engine.conversione(extension, targetFormat, inputFileForEngine, password, mergeImages, outputDirectoryForEngine);
                } else {
                    convertedFileFromEngine = engine.conversione(extension, targetFormat, inputFileForEngine, password, outputDirectoryForEngine);
                }
            } else {
                if (mergeImages && targetFormat.equals("jpg")) { // Controlla la logica per mergeImages e jpg
                    convertedFileFromEngine = engine.conversione(extension, targetFormat, inputFileForEngine, mergeImages, outputDirectoryForEngine);
                } else {
                    convertedFileFromEngine = engine.conversione(extension, targetFormat, inputFileForEngine, outputDirectoryForEngine);
                }
            }

            // Il file convertito è ora in convertedFileFromEngine
            convertedOutputFilePath = convertedFileFromEngine.toPath();

            // 4. Leggi i byte del file convertito
            byte[] fileBytes = Files.readAllBytes(convertedOutputFilePath);

            // 5. Determina il Content-Type corretto per la risposta HTTP
            MediaType contentType = determineMediaType(targetFormat);

            // 6. Costruisci la risposta con i byte del file
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(contentType);
            // Suggerisce un nome per il download al browser del client
            headers.setContentDispositionFormData("attachment", convertedFileFromEngine.getName());
            headers.setContentLength(fileBytes.length);

            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace(); // Stampa la traccia dello stack per il debug sul server
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Errore durante la conversione: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        } finally {
            // 7. Pulisci i file temporanei e la directory temporanea
            try {
                if (tempInputFilePath != null && Files.exists(tempInputFilePath)) {
                    Files.delete(tempInputFilePath);
                }
                if (convertedOutputFilePath != null && Files.exists(convertedOutputFilePath)) {
                    Files.delete(convertedOutputFilePath);
                }
                // Infine, elimina la directory temporanea che dovrebbe essere vuota
                if (conversionTempDir != null && Files.exists(conversionTempDir)) {
                    Files.delete(conversionTempDir); // Elimina la directory
                }
            } catch (IOException cleanupException) {
                System.err.println("Errore durante la pulizia dei file temporanei: " + cleanupException.getMessage());
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
            case "gif": return MediaType.IMAGE_GIF; // Aggiunto per completezza
            case "bmp": return MediaType.valueOf("image/bmp");
            case "tiff": return MediaType.valueOf("image/tiff");
            case "webp": return MediaType.valueOf("image/webp");
            case "zip": return MediaType.valueOf("application/zip");
            case "json": return MediaType.APPLICATION_JSON;
            case "xml": return MediaType.APPLICATION_XML;
            case "txt": return MediaType.TEXT_PLAIN;
            case "html": return MediaType.TEXT_HTML;
            case "doc":
            case "docx": return MediaType.valueOf("application/msword"); // O application/vnd.openxmlformats-officedocument.wordprocessingml.document
            case "xls":
            case "xlsx": return MediaType.valueOf("application/vnd.ms-excel"); // O application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
            case "ppt":
            case "pptx": return MediaType.valueOf("application/vnd.ms-powerpoint"); // O application/vnd.openxmlformats-officedocument.presentationml.presentation
            // Aggiungi altri casi se necessario
            default: return MediaType.APPLICATION_OCTET_STREAM; // Tipo generico per dati binari sconosciuti
        }
    }
}