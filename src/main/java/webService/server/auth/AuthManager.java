package webService.server.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Gestore dell'autenticazione lato client (Java 8 compatibile con OkHttp)
 */
public class AuthManager {

    private static final Logger logger = LogManager.getLogger(AuthManager.class);
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String authToken;
    private User currentUser;
    private LocalDateTime tokenExpiry;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public AuthManager(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();

        logger.info("AuthManager inizializzato per URL: {}", baseUrl);
    }

    /**
     * Effettua il login
     */
    public AuthResponse login(LoginRequest loginRequest) throws AuthException {
        try {
            logger.info("Tentativo di login per utente: {}", loginRequest.getUsername());

            String requestBody = objectMapper.writeValueAsString(loginRequest);

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/converter/login")
                    .post(RequestBody.create(JSON, requestBody))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    // Parse the JSON response manually since it's a Map structure
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> responseMap = objectMapper.readValue(responseBody, java.util.Map.class);

                    if (Boolean.TRUE.equals(responseMap.get("success"))) {
                        String token = (String) responseMap.get("token");
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> userMap = (java.util.Map<String, Object>) responseMap.get("user");
                        Long expiresAtMillis = ((Number) responseMap.get("expiresAt")).longValue();

                        // Convert user map to User object
                        User user = objectMapper.convertValue(userMap, User.class);

                        // Convert timestamp to LocalDateTime
                        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(expiresAtMillis),
                                java.time.ZoneId.systemDefault()
                        );

                        // Salva i dati di autenticazione
                        this.authToken = token;
                        this.currentUser = user;
                        this.tokenExpiry = expiresAt;

                        // Create AuthResponse
                        AuthResponse authResponse = new AuthResponse();
                        authResponse.setToken(token);
                        authResponse.setUser(user);
                        authResponse.setExpiresAt(expiresAt);

                        logger.info("Login riuscito per utente: {}", loginRequest.getUsername());
                        return authResponse;
                    } else {
                        String message = (String) responseMap.get("message");
                        throw new AuthException(message != null ? message : "Login failed");
                    }

                } else if (response.code() == 401) {
                    logger.warn("Login fallito - credenziali non valide per utente: {}", loginRequest.getUsername());
                    throw new AuthException("Credenziali non valide");

                } else {
                    logger.error("Errore server durante login: HTTP {}", response.code());
                    throw new AuthException("Errore del server durante l'autenticazione");
                }
            }

        } catch (IOException e) {
            logger.error("Errore di connessione durante login: {}", e.getMessage());
            throw new AuthException("Impossibile connettersi al server di autenticazione");
        }
    }

    /**
     * Registra un nuovo utente
     */
    public boolean register(RegisterRequest registerRequest) throws AuthException {
        try {
            logger.info("Tentativo di registrazione per utente: {}", registerRequest.getUsername());

            if (authToken == null) {
                throw new AuthException("È necessario essere autenticati per registrare nuovi utenti");
            }

            String requestBody = objectMapper.writeValueAsString(registerRequest);

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/converter/register")
                    .post(RequestBody.create(JSON, requestBody))
                    .addHeader("Authorization", "Bearer " + authToken)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.info("Registrazione riuscita per utente: {}", registerRequest.getUsername());
                    return true;

                } else if (response.code() == 400) {
                    // Estrai il messaggio di errore dal JSON di risposta
                    try {
                        String responseBody = response.body().string();
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> errorMap = objectMapper.readValue(responseBody, java.util.Map.class);
                        String message = (String) errorMap.get("message");
                        throw new AuthException(message != null ? message : "Dati di registrazione non validi");
                    } catch (Exception e) {
                        throw new AuthException("Dati di registrazione non validi");
                    }

                } else if (response.code() == 403) {
                    throw new AuthException("Non hai i permessi per registrare nuovi utenti");

                } else {
                    logger.error("Errore server durante registrazione: HTTP {}", response.code());
                    throw new AuthException("Errore del server durante la registrazione");
                }
            }

        } catch (IOException e) {
            logger.error("Errore di connessione durante registrazione: {}", e.getMessage());
            throw new AuthException("Impossibile connettersi al server");
        }
    }

    /**
     * Effettua il logout
     */
    public void logout() {
        try {
            if (authToken != null) {
                logger.info("Tentativo di logout per utente: {}", currentUser != null ? currentUser.getUsername() : "unknown");

                Request request = new Request.Builder()
                        .url(baseUrl + "/api/converter/logout")
                        .post(RequestBody.create(null, new byte[0])) // Empty body
                        .addHeader("Authorization", "Bearer " + authToken)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    logger.info("Logout completato con status: {}", response.code());
                }
            }
        } catch (Exception e) {
            logger.warn("Errore durante logout: {}", e.getMessage());
        } finally {
            // Pulisce i dati locali indipendentemente dal risultato della chiamata al server
            clearAuthData();
        }
    }

    /**
     * Verifica se l'utente è autenticato
     */
    public boolean isAuthenticated() {
        if (authToken == null || currentUser == null) {
            return false;
        }

        // Verifica se il token è scaduto
        if (tokenExpiry != null && LocalDateTime.now().isAfter(tokenExpiry)) {
            logger.info("Token scaduto, clearing auth data");
            clearAuthData();
            return false;
        }

        return true;
    }

    /**
     * Verifica la validità del token con il server
     */
    public boolean verifyToken() {
        if (!isAuthenticated()) {
            return false;
        }

        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/converter/verify")
                    .get()
                    .addHeader("Authorization", "Bearer " + authToken)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    logger.debug("Token verificato con successo");
                    return true;
                } else {
                    logger.warn("Token non valido, status: {}", response.code());
                    clearAuthData();
                    return false;
                }
            }

        } catch (Exception e) {
            logger.error("Errore durante verifica token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Ottiene l'utente corrente
     */
    public User getCurrentUser() throws AuthException {
        if (!isAuthenticated()) {
            throw new AuthException("Utente non autenticato");
        }
        return currentUser;
    }

    /**
     * Ottiene il token di autenticazione
     */
    public String getAuthToken() {
        return authToken;
    }

    /**
     * Imposta il token di autenticazione (per uso interno)
     */
    public void setAuthToken(String token) {
        this.authToken = token;
    }

    /**
     * Verifica se il servizio di autenticazione è disponibile
     */
    public boolean isServiceAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/converter/status")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }

        } catch (Exception e) {
            logger.debug("Servizio non disponibile: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Pulisce i dati di autenticazione
     */
    private void clearAuthData() {
        this.authToken = null;
        this.currentUser = null;
        this.tokenExpiry = null;
        logger.debug("Dati di autenticazione rimossi");
    }

    /**
     * Ottiene un header di autorizzazione per le richieste HTTP
     */
    public String getAuthorizationHeader() throws AuthException {
        if (!isAuthenticated()) {
            throw new AuthException("Utente non autenticato");
        }
        return "Bearer " + authToken;
    }
}