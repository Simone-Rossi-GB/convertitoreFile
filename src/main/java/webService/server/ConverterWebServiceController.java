package webService.server;

import org.apache.jena.reasoner.IllegalParameterException;
import webService.server.auth.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import webService.server.config.configHandlers.Config;
import webService.server.converters.exception.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.apache.tika.Tika;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Autowired
    private AuthService authService;

    /**
     * Metodo helper per estrarre e validare il token dall'header Authorization
     */
    private User validateAuthHeader(String authHeader) throws AuthException {
        if (authHeader == null || authHeader.isEmpty()) {
            throw new AuthException("il token è richiesto");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new AuthException("header token non valido. header atteso: Bearer <token>");
        }

        String token = authHeader.substring(7); // Rimuove "Bearer "
        return authService.getCurrentUser(token);
    }

    /**
     * @return lo stato del web service - NON RICHIEDE AUTENTICAZIONE
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getStatus() {
        logger.info("WebService: Richiesta stato ricevuta");
        return ResponseEntity.ok(Collections.singletonMap("status", "active"));
    }

    /**
     * Ritorna le possibili conversioni in base al file .json - RICHIEDE AUTENTICAZIONE
     * @param extension estensione di partenza
     * @param authHeader header Authorization con Bearer token
     * @return lista delle conversioni possibili
     */
    @GetMapping("/conversions/{extension}")
    public ResponseEntity<?> getPossibleConversions(
            @PathVariable String extension,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Valida il token
            User user = validateAuthHeader(authHeader);
            logger.info("Richiesta conversioni possibili per estensione: {} da utente: {}", extension, user.getUsername());

            List<String> conversions = engine.getPossibleConversions(extension);
            return ResponseEntity.ok(conversions);

        } catch (AuthException e) {
            logger.warn("Accesso negato a getPossibleConversions: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            logger.error("Errore in getPossibleConversions: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Effettua la conversione di un file - RICHIEDE AUTENTICAZIONE
     * @param file file da convertire
     * @param configuration configurazione SENZA token (viene preso dall'header)
     * @param authHeader header Authorization con Bearer token
     * @return file convertito o errore
     */
    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> convertFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart("config") Config configuration,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Valida il token dall'header invece che dal JSON
            User user = validateAuthHeader(authHeader);
            logger.info("Inizio conversione file: {} -> {} per utente: {}",
                    file.getOriginalFilename(), configuration.getData().getDestinationFormat(), user.getUsername());

            Path tempInputFilePath = null;
            Path conversionTempDir = null;
            File convertedOutputFile = null;

            // Crea una directory temporanea con identificativo univoco per questa conversione
            conversionTempDir = Files.createTempDirectory("conversion-" + UUID.randomUUID() + "-");
            logger.info("Creata directory temporanea: {}", conversionTempDir);

            // Salva il file caricato nella directory temporanea
            String originalFilename = file.getOriginalFilename();
            String extension = Utility.getExtension(new File(Objects.requireNonNull(file.getOriginalFilename())));

            assert originalFilename != null;
            tempInputFilePath = conversionTempDir.resolve(originalFilename);
            file.transferTo(tempInputFilePath);
            logger.info("File salvato in: {}", tempInputFilePath);

            // Creiamo un oggetto file dal percorso del file
            File inputFileForEngine = tempInputFilePath.toFile();
            logger.info(inputFileForEngine.getAbsolutePath());

            // Chiama EngineWebService per la conversione
            convertedOutputFile = engine.conversione(extension, configuration, inputFileForEngine);

            logger.info("File convertito dal motore: {}", convertedOutputFile != null ? convertedOutputFile.getAbsolutePath() : "NULL");
            if (convertedOutputFile != null) {
                logger.info("File esiste: {}", convertedOutputFile.exists());
                logger.info("Dimensione file: {}", convertedOutputFile.length());
            }

            // Verifica che il file convertito esista
            if (convertedOutputFile == null || !convertedOutputFile.exists()) {
                throw new ConversionException("File convertito inesistente");
            }

            // Crea un array di byte per la risposta al client
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
            clearTempFiles(tempInputFilePath, convertedOutputFile, conversionTempDir);
            return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);

        } catch (AuthException e) {
            logger.warn("Accesso negato a convertFile: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            logger.error("Errore durante conversione: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Conversion failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Elimina i vari file e directory temporanee
     */
    private void clearTempFiles(Path tempInputFilePath, File convertedOutputFile, Path conversionTempDir) {
        try {
            if (tempInputFilePath != null && Files.exists(tempInputFilePath)) {
                Files.delete(tempInputFilePath);
                logger.info("File temporaneo di input eliminato");
            }
            if (convertedOutputFile != null && Files.exists(convertedOutputFile.toPath())) {
                Files.delete(convertedOutputFile.toPath());
                logger.info("File temporaneo di output eliminato");
            }

            if (conversionTempDir != null && Files.exists(conversionTempDir)) {
                eliminaContenuto(conversionTempDir.toFile());
                logger.info("Directory temporanea svuotata");
                Files.delete(conversionTempDir);
                logger.info("Directory temporanea eliminata");
            }
        } catch (IOException cleanupException) {
            logger.error("Errore durante la pulizia dei file temporanei: " + cleanupException.getMessage());
        }
    }

    /**
     * Elimina il contenuto di una cartella in maniera ricorsiva
     */
    private void eliminaContenuto(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    eliminaContenuto(file);
                }
                file.delete();
            }
        }
    }

    /**
     * Metodo che riconosce automaticamente il MediaType dal nome del file tramite libreria Tika
     */
    private MediaType determineMediaType(File file) {
        try {
            String mimeType = tika.detect(file);
            logger.warn(mimeType);
            return MediaType.parseMediaType(mimeType);
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * Endpoint per il login con app client - NON RICHIEDE AUTENTICAZIONE
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Tentativo di login per utente: {}", loginRequest.getUsername());

        try {
            AuthResponse authResponse = authService.authenticate(loginRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", authResponse.getToken());
            response.put("user", authResponse.getUser());
            response.put("expiresAt", authResponse.getExpiresAt());

            logger.info("Login riuscito per utente: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (AuthException e) {
            logger.warn("Login fallito per utente {}: {}", loginRequest.getUsername(), e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            logger.error("Errore interno durante login per utente {}: {}", loginRequest.getUsername(), e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint per il login senza app client
     */
    @PostMapping("/loginwa")
    public ResponseEntity<Map<String, Object>> loginwa(@Valid @RequestBody String username, @RequestBody String password) {
        logger.info("Tentativo di login per utente: {}", username);

        try {
            LoginRequest loginRequest = new LoginRequest(username, password);
            AuthResponse authResponse = authService.authenticate(loginRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", authResponse.getToken());
            response.put("user", authResponse.getUser());
            response.put("expiresAt", authResponse.getExpiresAt());

            logger.info("Login riuscito per utente: {}", loginRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (AuthException e) {
            logger.warn("Login fallito per utente {}: {}", username, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            logger.error("Errore interno durante login per utente {}: {}", username, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint per il logout - RICHIEDE AUTENTICAZIONE
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = validateAuthHeader(authHeader);
            logger.info("Logout per utente: {}", user.getUsername());

            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Logout successful");

            return ResponseEntity.ok(response);

        } catch (AuthException e) {
            logger.error("Errore durante logout: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            logger.error("Errore durante logout: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "Logout failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint per registrare un nuovo utente - REGISTRAZIONE PUBBLICA O DA ADMIN
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            logger.info("Tentativo di registrazione per utente: {}", registerRequest.getUsername());

            // Registrazione pubblica con controllo se c'è un token valido
            if (authHeader != null && !authHeader.isEmpty()) {
                try {
                    User currentUser = validateAuthHeader(authHeader);

                    // Se sei già autenticato e non sei admin, non puoi registrare altri
                    if (!"ADMIN".equals(currentUser.getRole())) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Access denied. Admin role required for authenticated registration.");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                    }
                } catch (AuthException e) {
                    // Token non valido, procedi con registrazione pubblica
                    logger.debug("Token non valido per registrazione: {}", e.getMessage());
                }
            }

            // Registrazione pubblica o da admin
            User newUser = authService.registerUser(registerRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("user", new UserDTO(newUser));

            logger.info("Nuovo utente registrato: {}", newUser.getUsername());
            return ResponseEntity.ok(response);

        } catch (AuthException e) {
            logger.warn("Registrazione fallita per {}: {}", registerRequest.getUsername(), e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            logger.error("Errore durante registrazione per {}: {}", registerRequest.getUsername(), e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint per verificare la validità del token - RICHIEDE AUTENTICAZIONE
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            User user = validateAuthHeader(authHeader);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("user", new UserDTO(user));

            return ResponseEntity.ok(response);

        } catch (AuthException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "Invalid token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}