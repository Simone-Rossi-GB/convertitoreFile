package webService.client.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AuthManager con DTO corretti per comunicare con il server
 */
public class AuthManager {

    private static final Logger logger = LogManager.getLogger(AuthManager.class);
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // UNICA cosa che salviamo
    private String jwtToken;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public AuthManager(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();

        logger.info("AuthManager inizializzato per: {}", baseUrl);
    }

    /**
     * Login - usa LoginRequest DTO
     */
    public boolean login(String username, String password) {
        try {
            // Crea LoginRequest DTO
            LoginRequest loginRequest = new LoginRequest(username, password);

            // Serializza in JSON con Jackson
            String json = objectMapper.writeValueAsString(loginRequest);

            logger.debug("JSON inviato per login: {}", json);

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/converter/login")
                    .post(RequestBody.create(JSON, json))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                logger.debug("Response code: {}", response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    logger.debug("Response body: {}", responseBody);

                    // Parse JSON response
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                    if (Boolean.TRUE.equals(responseMap.get("success"))) {
                        // Salva SOLO il token
                        this.jwtToken = (String) responseMap.get("token");
                        logger.info("Login riuscito per: {}", username);
                        return true;
                    } else {
                        logger.warn("Login fallito per {}: {}", username, responseMap.get("message"));
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    logger.warn("Login fallito per {} - HTTP {}: {}", username, response.code(), errorBody);
                }

                return false;
            }

        } catch (IOException e) {
            logger.error("Errore connessione durante login per {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Register - usa RegisterRequest DTO
     */
    public boolean register(String fullName, String username, String email, String password, String role) {
        try {
            // Crea RegisterRequest DTO
            RegisterRequest registerRequest = new RegisterRequest(fullName, username, email, password, role);

            // Serializza in JSON con Jackson
            String json = objectMapper.writeValueAsString(registerRequest);

            logger.debug("JSON inviato per registrazione: {}", json);

            // Registrazione sempre senza header Authorization
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/converter/register")
                    .post(RequestBody.create(JSON, json))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                logger.debug("Response code: {}", response.code());

                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    logger.debug("Response body: {}", responseBody);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

                    if (Boolean.TRUE.equals(responseMap.get("success"))) {
                        logger.info("Registrazione riuscita per: {}", username);
                        return true;
                    } else {
                        logger.warn("Registrazione fallita per {}: {}", username, responseMap.get("message"));
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    logger.warn("Registrazione fallita per {} - HTTP {}: {}", username, response.code(), errorBody);
                }

                return false;
            }

        } catch (IOException e) {
            logger.error("Errore connessione durante registrazione per {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Logout - cancella token locale
     */
    public void logout() {
        if (jwtToken != null) {
            try {
                // Notifica il server (opzionale)
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/converter/logout")
                        .post(RequestBody.create(null, new byte[0]))
                        .addHeader("Authorization", "Bearer " + jwtToken)
                        .build();

                httpClient.newCall(request).execute().close();

                logger.info("logout effettuato");

            } catch (IOException e) {
                logger.debug("Errore durante logout: {}", e.getMessage());
            }
        }

        // cancella il token
        this.jwtToken = null;
        logger.info("Logout completato");
    }

    /**
     * Verifica se autenticato
     */
    public boolean isAuthenticated() {
        return jwtToken != null && !jwtToken.isEmpty();
    }

    /**
     * Ottiene header Authorization per altre richieste
     */
    public String getAuthorizationHeader() {
        if (!isAuthenticated()) {
            return null;
        }
        return "Bearer " + jwtToken;
    }

    /**
     * Verifica se il server è disponibile
     */
    public boolean isServerAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(baseUrl + "/api/converter/status")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }

        } catch (IOException e) {
            return false;
        }
    }
}