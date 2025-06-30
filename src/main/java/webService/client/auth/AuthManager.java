package webService.client.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.security.auth.message.AuthException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Gestore dell'autenticazione lato client
 */
public class AuthManager {

    private static final Logger logger = LogManager.getLogger(AuthManager.class);
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private String authToken;
    private User currentUser;
    private LocalDateTime tokenExpiry;

    public AuthManager(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
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

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                AuthResponse authResponse = objectMapper.readValue(response.body(), AuthResponse.class);

                // Salva i dati di autenticazione
                this.authToken = authResponse.getToken();
                this.currentUser = authResponse.getUser();
                this.tokenExpiry = authResponse.getExpiresAt();

                logger.info("Login riuscito per utente: {}", loginRequest.getUsername());
                return authResponse;

            } else if (response.statusCode() == 401) {
                logger.warn("Login fallito - credenziali non valide per utente: {}", loginRequest.getUsername());
                throw new AuthException("Credenziali non valide");

            } else {
                logger.error("Errore server durante login: HTTP {}", response.statusCode());
                throw new AuthException("Errore del server durante l'autenticazione");
            }

        } catch (IOException | InterruptedException e) {
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

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/register"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + authToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.info("Registrazione riuscita per utente: {}", registerRequest.getUsername());
                return true;

            } else if (response.statusCode() == 400) {
                // Estrai il messaggio di errore dal JSON di risposta
                try {
                    ErrorResponse errorResponse = objectMapper.readValue(response.body(), ErrorResponse.class);
                    throw new AuthException(errorResponse.getMessage());
                } catch (Exception e) {
                    throw new AuthException("Dati di registrazione non validi");
                }

            } else if (response.statusCode() == 403) {
                throw new AuthException("Non hai i permessi per registrare nuovi utenti");

            } else {
                logger.error("Errore server durante registrazione: HTTP {}", response.statusCode());
                throw new AuthException("Errore del server durante la registrazione");
            }

        } catch (IOException | InterruptedException e) {
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

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/auth/logout"))
                        .header("Authorization", "Bearer " + authToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(10))
                        .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.info("Logout completato");
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/auth/verify"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                logger.debug("Token verificato con successo");
                return true;
            } else {
                logger.warn("Token non valido, status: {}", response.statusCode());
                clearAuthData();
                return false;
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/converter/status"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

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