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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConverterWebServiceClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public ConverterWebServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Controlla la disponibilità del servizio web.
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
     * @param extension L'estensione del file sorgente (es. "pdf").
     * @return Una lista di estensioni di destinazione possibili.
     * @throws Exception Se la richiesta fallisce o il servizio non è disponibile.
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
     * Invia un file per la conversione al servizio web e salva il risultato localmente.
     *
     * @param inputFile Il file sorgente da convertire.
     * @param targetFormat Il formato di destinazione desiderato.
     * @param outputFile Il percorso completo dove salvare il file convertito localmente.
     * @param password La password per i file protetti (opzionale, null se non presente).
     * @param mergeImages Indica se unire le immagini (opzionale, false se non applicabile).
     * @return Un oggetto ConversionResult che indica il successo e un messaggio.
     */
    public ConversionResult convertFile(File inputFile, String targetFormat, File outputFile, String password, boolean mergeImages) {
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

            if (password != null && !password.isEmpty()) {
                body.add("password", password); // Aggiunge la password se presente
            }
            body.add("mergeImages", String.valueOf(mergeImages)); // Aggiunge il flag mergeImages

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Esegui la richiesta POST e aspetta un array di byte come risposta
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
                return new ConversionResult(true, "File convertito e salvato con successo: " + outputFile.getName());
            } else {
                // Se la risposta non è 2xx OK
                String errorMessage = "Errore durante la conversione: " + response.getStatusCode();
                if (response.getBody() != null) {
                    // Tenta di decodificare l'errore JSON dal body se presente
                    try {
                        String responseBody = new String(response.getBody());
                        // Potresti voler usare una libreria JSON come Gson o Jackson qui
                        // per parsare un oggetto errore, ma per semplicità ora lo stampo
                        System.err.println("Errore dal server: " + responseBody);
                        errorMessage += " - Dettagli: " + responseBody;
                    } catch (Exception e) {
                        // Impossibile leggere il body come stringa o JSON
                        errorMessage += " (corpo risposta illeggibile)";
                    }
                }
                return new ConversionResult(false, errorMessage);
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Errori HTTP specifici (4xx, 5xx)
            String errorMessage = "Errore del server (" + e.getStatusCode() + "): " + e.getResponseBodyAsString();
            System.err.println(errorMessage);
            return new ConversionResult(false, errorMessage);
        } catch (ResourceAccessException e) {
            // Errore di rete o connessione
            String errorMessage = "Errore di connessione al servizio: " + e.getMessage();
            System.err.println(errorMessage);
            return new ConversionResult(false, errorMessage);
        } catch (IOException e) {
            // Errore nella scrittura del file locale
            String errorMessage = "Errore I/O durante il salvataggio del file convertito: " + e.getMessage();
            System.err.println(errorMessage);
            return new ConversionResult(false, errorMessage);
        } catch (Exception e) {
            // Qualsiasi altro errore inatteso
            String errorMessage = "Errore inatteso durante la conversione: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace(); // Per debug
            return new ConversionResult(false, errorMessage);
        }
    }
}