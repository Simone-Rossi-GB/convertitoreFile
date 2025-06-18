package WebService.controller;

import converter.Engine;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/converter")
@CrossOrigin(origins = "*")
public class ConverterWebServiceController {

    private final Engine engine = new Engine();

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        return ResponseEntity.ok(Map.of("status", "active"));
    }

    @GetMapping("/conversions/{extension}")
    public ResponseEntity<List<String>> getPossibleConversions(@PathVariable String extension) {
        try {
            List<String> conversions = engine.getPossibleConversions(extension);
            return ResponseEntity.ok(conversions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<Map<String, Object>> convertFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetFormat") String targetFormat,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "mergeImages", required = false, defaultValue = "false") boolean mergeImages) {

        String conversionId = UUID.randomUUID().toString();

        try {
            // Crea directory temporanea se non esiste
            String tempDir = "temp/uploads/";
            Files.createDirectories(Paths.get(tempDir));

            // Salva file temporaneo
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);

            File tempFile = new File(tempDir + conversionId + "_" + originalFilename);
            file.transferTo(tempFile);

            // Chiama l'Engine per conversione con i parametri giusti
            if (password != null && !password.trim().isEmpty()) {
                if (mergeImages && targetFormat.equals("jpg")) {
                    engine.conversione(extension, targetFormat, tempFile, password, mergeImages);
                } else {
                    engine.conversione(extension, targetFormat, tempFile, password);
                }
            } else {
                if (mergeImages && targetFormat.equals("jpg")) {
                    engine.conversione(extension, targetFormat, tempFile, mergeImages);
                } else {
                    engine.conversione(extension, targetFormat, tempFile);
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Conversione completata con successo",
                    "conversionId", conversionId
            ));

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}