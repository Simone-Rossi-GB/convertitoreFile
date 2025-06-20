package WebService.client;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ConverterWebServiceClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;
    private static final Logger logger = LogManager.getLogger(ConverterWebServiceClient.class);

    public ConverterWebServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Controlla la disponibilità del servizio web.
     */
    public boolean isServiceAvailable() {
        try {
            String url = baseUrl + "/api/converter/status";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null &&
                    "active".equals(response.getBody().get("status"));
        } catch (ResourceAccessException e) {
            System.err.println("Servizio web non disponibile (connessione fallita): " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Errore durante il controllo dello stato del servizio web: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recupera la lista delle conversioni possibili per una data estensione sorgente.
     */
    public List<String> getPossibleConversions(String extension) throws Exception {
        if (!isServiceAvailable()) {
            throw new Exception("Servizio di conversione non disponibile.");
        }
        try {
            String url = baseUrl + "/api/converter/conversions/" + extension;
            ResponseEntity<String[]> response = restTemplate.getForEntity(url, String[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            } else {
                throw new Exception("Errore nel recupero delle conversioni possibili: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new Exception("Errore HTTP durante il recupero delle conversioni: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new Exception("Errore sconosciuto durante il recupero delle conversioni: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica i requisiti di conversione per un file
     */
    public Map<String, Object> checkConversionRequirements(File inputFile) throws Exception {
        if (!isServiceAvailable()) {
            throw new Exception("Servizio di conversione non disponibile.");
        }

        try {
            String url = baseUrl + "/api/converter/check-requirements";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(inputFile));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new Exception("Errore durante il controllo dei requisiti: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new Exception("Errore nel controllo dei requisiti: " + e.getMessage(), e);
        }
    }

    /**
     * Conversione senza password
     */
    public ConversionResult convertFile(File inputFile, String targetFormat, File outputFile) {
        return convertFileWithPassword(inputFile, targetFormat, outputFile, null);
    }

    /**
     * Conversione con password opzionale
     */
    public ConversionResult convertFileWithPassword(File inputFile, String targetFormat, File outputFile, String password) {
        return convertFileWithAllParameters(inputFile, targetFormat, outputFile, password, false);
    }

    /**
     * Conversione con tutti i parametri
     */
    public ConversionResult convertFileWithAllParameters(File inputFile, String targetFormat, File outputFile,
                                                         String password, boolean booleanOption) {
        if (!isServiceAvailable()) {
            return ConversionResult.error("Servizio di conversione non disponibile.");
        }

        try {
            String url = baseUrl + "/api/converter/convert";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(inputFile));
            body.add("targetFormat", targetFormat);

            if (password != null && !password.isEmpty()) {
                body.add("password", password);
            }
            body.add("booleanOption", String.valueOf(booleanOption));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Scrive i byte ricevuti nel file di output specificato
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(response.getBody());
                }
                return ConversionResult.success("File convertito e salvato con successo: " + outputFile.getName());
            } else {
                return ConversionResult.error("Errore durante la conversione: " + response.getStatusCode());
            }

        } catch (HttpClientErrorException e) {
            // Gestione specifica per errori 400 (Bad Request) che potrebbero indicare password richiesta
            if (e.getStatusCode().value() == 400) {
                String responseBody = e.getResponseBodyAsString();
                if (responseBody.contains("PASSWORD_REQUIRED")) {
                    return new ConversionResult(false, null, "PASSWORD_REQUIRED: Il file richiede una password");
                }
            }
            String errorMessage = "Errore del client (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
            logger.error(errorMessage);
            return ConversionResult.error(errorMessage);

        } catch (HttpServerErrorException e) {
            String errorMessage = "Errore del server (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
            logger.error(errorMessage);
            return ConversionResult.error(errorMessage);

        } catch (ResourceAccessException e) {
            String errorMessage = "Errore di connessione al servizio: " + e.getMessage();
            logger.error(errorMessage);
            return ConversionResult.error(errorMessage);

        } catch (IOException e) {
            String errorMessage = "Errore I/O durante il salvataggio del file convertito: " + e.getMessage();
            logger.error(errorMessage);
            return ConversionResult.error(errorMessage);

        } catch (Exception e) {
            String errorMessage = "Errore inatteso durante la conversione: " + e.getMessage();
            logger.error(errorMessage);
            e.printStackTrace();
            return ConversionResult.error(errorMessage);
        }
    }
}