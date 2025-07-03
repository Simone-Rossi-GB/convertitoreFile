package webService.server;

import org.apache.jena.reasoner.IllegalParameterException;
import webService.server.auth.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import webService.server.config.configHandlers.conversionContext.ConversionContextReader;
import webService.server.converters.exception.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.apache.tika.Tika;
import webService.server.config.configHandlers.conversionContext.ConversionContextData;
import webService.server.config.configHandlers.conversionContext.ConversionContextInstance;

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
        logger.info("Richiesta conversioni possibili per estensione: {}", extension);
        List<String> conversions = engine.getPossibleConversions(extension);
        // ritorno un json contenente una lista con le possibili conversioni da un determinato formato d'origine
        return ResponseEntity.ok(conversions);
    }

    /**
     * Effettua la conversione di un file
     * @param file file da convertire
     * @param targetFormat formato di destinazione
     * @return Response entity ok, con un array di byte che rappresenta il contenuto del file convertito, il content type, il nome e la lunghezza.
     * In caso di errori ritora un internalServerError con un messaggio di spiegazione.
     */
    @PostMapping("/convert")
    public ResponseEntity<?> convertFile(
            @RequestParam("file") MultipartFile file, //parametri richiesti dopo il /convert/
            @RequestParam("targetFormat") String targetFormat ,
            @RequestParam("configFile") MultipartFile configFile)throws IOException, FileMoveException { //Json di configurazione

        Path tempInputFilePath = null;
        Path conversionTempDir = null;
        File convertedOutputFile = null;
        logger.info("Inizio conversione file: {} -> {}", file.getOriginalFilename(), targetFormat);

        // Crea una directory temporanea con identificativo univoco per questa conversione
        conversionTempDir = Files.createTempDirectory("conversion-" + UUID.randomUUID() + "-");
        logger.info("Creata directory temporanea: {}", conversionTempDir);
        configFileUpload(configFile, conversionTempDir);
        logger.warn(ConversionContextReader.getDestinationFormat());
        // Salva il file caricato nella directory temporanea
        String originalFilename = file.getOriginalFilename();
        String extension = null;

        // otteniamo direttamente dal file ricevuto il formato d'origine
        extension = Utility.getExtension(new File(Objects.requireNonNull(file.getOriginalFilename())));

        assert originalFilename != null; // diciamo al JVM che il nome non è mai null

        // prendiamo la directory temporanea e col metodo resolve mettiamo in
        // tempInputFilePath  percorsoTemporaneo/originalFileName.estensione
        tempInputFilePath = conversionTempDir.resolve(originalFilename);

        file.transferTo(tempInputFilePath); // spostiamo il file nella director y temporanea
        logger.info("File salvato in: {}", tempInputFilePath);

        // creiamo un oggetto file dal percorso del file
        File inputFileForEngine = tempInputFilePath.toFile();
        logger.info(inputFileForEngine.getAbsolutePath());
        // Chiama EngineWebService per la conversione
        convertedOutputFile = engine.conversione(extension, targetFormat, inputFileForEngine);

        logger.info("File convertito dal motore: {}", convertedOutputFile != null ? convertedOutputFile.getAbsolutePath() : "NULL");
        if (convertedOutputFile != null) {
            logger.info("File esiste: {}", convertedOutputFile.exists());
            logger.info("Dimensione file: {}", convertedOutputFile.length());
        }

        // Verifica che il file convertito esista
        if (convertedOutputFile == null || !convertedOutputFile.exists()) {
            throw new ConversionException("File convertito inesistente");
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
        clearTempFiles(tempInputFilePath, convertedOutputFile, conversionTempDir);
        return new ResponseEntity<>(fileBytes, headers, HttpStatus.OK);
    }


    /**
     * Elimina i vari file e directory temporanee
     * @param tempInputFilePath file temporaneo di input
     * @param convertedOutputFile file convertito
     * @param conversionTempDir cartella temporanea
     */
    private void clearTempFiles(Path tempInputFilePath, File convertedOutputFile, Path conversionTempDir){
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



    /**
     * Salva il file in un'apposita cartella o lo sostituisce se esiste già
     * @param file file di configurazione
     */
    private void configFileUpload (MultipartFile file, Path tempDirectory) throws FileMoveException {
        if (file.isEmpty()) {
            throw new IllegalParameterException("Il file di configurazione è vuoto");
        }
        try {
            Path filePath = tempDirectory.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            file.transferTo(filePath);
            ConversionContextInstance cci = new ConversionContextInstance(filePath.toFile());
            ConversionContextData.update(cci);
            logger.info("Cofigurazione conversion config caricata con successo");
        } catch (IOException e) {
            throw new FileMoveException("Impossibile salvare il file di configurazione conversion context");
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
            logger.warn(mimeType);
            return MediaType.parseMediaType(mimeType);
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    @Autowired
    private AuthService authService;

    /**
     * Endpoint per il login
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
     * Endpoint per il logout
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            authService.logout(token);

            Map<String, String> response = new HashMap<>();
            response.put("success", "true");
            response.put("message", "Logout successful");

            logger.info("Logout riuscito");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Errore durante logout: {}", e.getMessage());

            Map<String, String> response = new HashMap<>();
            response.put("success", "false");
            response.put("message", "Logout failed");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint per registrare un nuovo utente
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterRequest registerRequest,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            logger.info("Tentativo di registrazione per utente: {}", registerRequest.getUsername());

            // registrazione pubblica con controllo se c'è un token valido
            if (authHeader != null && !authHeader.isEmpty()) {
                try {
                    String token = authHeader.replace("Bearer ", "");
                    User currentUser = authService.getCurrentUser(token);

                    // Se sei già autenticato e non sei admin, non puoi registrare altri
                    if (!"ADMIN".equals(currentUser.getRole())) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", false);
                        response.put("message", "Access denied. Admin role required for authenticated registration.");
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
                    }
                } catch (Exception e) {
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
     * Endpoint per verificare la validità del token
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            User user = authService.getCurrentUser(token);

            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("user", new UserDTO(user));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "Invalid token");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
}