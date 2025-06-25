package webService.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
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

import javax.xml.ws.WebServiceException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
     *
     * @return true se il servizio è disponibile, false altrimenti.
     */
    public boolean isServiceAvailable() {
        try {
            String url = baseUrl + "/api/converter/status";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode().is2xxSuccessful() && response.getBody() != null && "active".equals(response.getBody().get("status"));
        } catch (ResourceAccessException e) {
            // Non riuscito a connettersi al server (e.g., server down, indirizzo errato)
            System.err.println("Servizio web non disponibile (connessione fallita): " + e.getMessage());
            return false;
        } catch (Exception e) {
            // Altri errori (e.g., server risponde con errore HTTP)
            System.err.println("Errore durante il controllo dello stato del servizio web: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recupera la lista delle conversioni possibili per una data estensione sorgente.
     *
     * @param extension L'estensione del file sorgente (es. "pdf").
     * @return Una lista di estensioni di destinazione possibili.
     */
    public List<String> getPossibleConversions(String extension) throws WebServiceException {
        if (!isServiceAvailable()) {
            throw new WebServiceException("Servizio di conversione non disponibile.");
        }
            String url = baseUrl + "/api/converter/conversions/" + extension;
            ResponseEntity<String[]> response = restTemplate.getForEntity(url, String[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            } else {
                throw new WebServiceException(response.getBody()[0]);
            }
    }

    /**
     * Invia un file per la conversione al servizio web e salva il risultato localmente.
     *
     * @param inputFile    Il file sorgente da convertire.
     * @param targetFormat Il formato di destinazione desiderato.
     * @param outputFile   Il percorso completo dove salvare il file convertito localmente.
     * @return Un oggetto ConversionResult che indica il successo e un messaggio.
     */
    public ConversionResult convertFile(File inputFile, String targetFormat, File outputFile) {
        if (!isServiceAvailable()) {
            return new ConversionResult(false, "Servizio di conversione non disponibile.");
        }

        try {
            String url = baseUrl + "/api/converter/convert";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(inputFile)); // Aggiunge il file
            body.add("targetFormat", targetFormat); // Aggiunge il formato target
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Esegui la richiesta POST e aspetta un array di byte come risposta
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Costruisce il nome corretto del file
                String baseName = outputFile.getName().replaceFirst("\\.[^\\.]+$", "");
                String extension = getExtensionFromMediaType(response.getHeaders().getContentType().toString());
                File correctedFile = new File(outputFile.getParent(), baseName + extension);
                Files.move(outputFile.toPath(), correctedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                // Scrive i byte ricevuti nel file di output specificato
                try (FileOutputStream fos = new FileOutputStream(correctedFile)) {
                    fos.write(response.getBody());
                }
                return new ConversionResult(true, "File convertito e salvato con successo: " + outputFile.getName());
            } else {
                // Se la risposta non è 2xx OK
                String errorMessage = "Errore durante la conversione: " + response.getStatusCode();
                logger.error(errorMessage);
                return new ConversionResult(false, errorMessage);
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Errori HTTP specifici (4xx, 5xx)
            return recordError("Errore del server (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            // Errore di rete o connessione
            return recordError("Errore di connessione al servizio: " + e.getMessage());
        } catch (IOException e) {
            // Errore nella scrittura del file locale
            return recordError("Errore I/O durante il salvataggio del file convertito: " + e.getMessage());
        } catch (Exception e) {
            // Qualsiasi altro errore inatteso
            return recordError("Errore inatteso durante la conversione: " + e.getMessage());
        }
    }

    private String getExtensionFromMediaType(String mediaType) {
        try {
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            MimeType mimeType = allTypes.forName(mediaType);
            return mimeType.getExtension();
        } catch (Exception e) {
            return ".bin"; // fallback
        }
    }

    private ConversionResult recordError(String message) {
        logger.error(message);
        return new ConversionResult(false, message);
    }

    public void sendConfigFile(File jsonFile){
        String url = baseUrl + "/api/converter/configUpload";

        if (!jsonFile.exists()) {
            System.err.println("Il file non esiste: " + jsonFile.getPath());
            return;
        }

        // Prepara il contenuto come multipart/form-data
        FileSystemResource resource = new FileSystemResource(jsonFile);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Invia POST
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        // Controlla se lo status HTTP è 2xx (es. 200 OK)
        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Upload riuscito: " + response.getBody());
        } else {
            logger.error("Upload fallito: " + response.getBody());
        }
    }

    public void sendConversionContextFile(File jsonFile){
        String url = baseUrl + "/api/converter/conversionContextUpload";

        if (!jsonFile.exists()) {
            System.err.println("Il file non esiste: " + jsonFile.getPath());
            return;
        }

        // Prepara il contenuto come multipart/form-data
        FileSystemResource resource = new FileSystemResource(jsonFile);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // Invia POST
        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        // Controlla se lo status HTTP è 2xx (es. 200 OK)
        if (response.getStatusCode().is2xxSuccessful()) {
            logger.info("Upload riuscito: " + response.getBody());
        } else {
            logger.error("Upload fallito: " + response.getBody());
        }
    }
}

