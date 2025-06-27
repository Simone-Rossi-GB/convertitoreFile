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
import webService.server.Utility;

import javax.xml.ws.WebServiceException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Client per la comunicazione con il servizio web di conversione file.
 * Gestisce tutte le operazioni di comunicazione HTTP con il server,
 * inclusi controlli di stato, conversioni file e upload di configurazioni.
 */
public class ConverterWebServiceClient {

    private final String baseUrl; // URL base del servizio web
    private final RestTemplate restTemplate; // Template per le chiamate HTTP REST
    private static final Logger logger = LogManager.getLogger(ConverterWebServiceClient.class);

    /**
     * Costruttore che inizializza il client con l'URL del webservice
     * @param baseUrl URL base del servizio web di conversione
     */
    public ConverterWebServiceClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Controlla la disponibilità del servizio web chiamando l'endpoint /status.
     *
     * @return true se il servizio è disponibile e attivo, false altrimenti
     */
    public boolean isServiceAvailable() {
        try {
            // Costruisce l'URL per il controllo dello stato del webservice
            String url = baseUrl + "/api/converter/status";

            // Effettua una chiamata GET per verificare lo stato del servizio
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            // Verifica che la risposta sia 2xx (succesful) e contenga status="active"
            return response.getStatusCode().is2xxSuccessful() &&
                    response.getBody() != null &&
                    "active".equals(response.getBody().get("status"));

        } catch (ResourceAccessException e) {
            // Errore di connessione (server down, indirizzo errato, ecc.)
            System.err.println("Servizio web non disponibile (connessione fallita): " + e.getMessage());
            return false;
        } catch (Exception e) {
            // Altri errori HTTP o di parsing della risposta
            System.err.println("Errore durante il controllo dello stato del servizio web: " + e.getMessage());
            return false;
        }
    }

    /**
     * Recupera la lista delle conversioni possibili per una data estensione sorgente.
     * Chiama l'endpoint /conversions/{extension} del servizio web.
     * @param extension L'estensione del file originale (es. "pdf", "docx")
     * @return Una lista di estensioni di destinazione possibili per la conversione
     * @throws WebServiceException se il servizio non è disponibile o si verifica un errore
     */
    public List<String> getPossibleConversions(String extension) throws WebServiceException {
        // Verifica preliminare della disponibilità del servizio
        if (!isServiceAvailable()) {
            throw new WebServiceException("Servizio di conversione non disponibile.");
        }

        // Costruisce l'URL per ottenere le conversioni possibili
        String url = baseUrl + "/api/converter/conversions/" + extension;

        // Effettua la chiamata GET e riceve un array di stringhe
        ResponseEntity<String[]> response = restTemplate.getForEntity(url, String[].class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            // Converte l'array in lista e lo ritorna
            return Arrays.asList(response.getBody());
        } else {
            // In caso di errore, lancia un'eccezione con il messaggio di errore
            throw new WebServiceException(response.getBody()[0]);
        }
    }

    /**
     * Invia un file per la conversione al servizio web e salva il risultato localmente.
     * Gestisce l'intero processo di upload, conversione e download del file risultante.
     *
     * @param inputFile    Il file originale da convertire
     * @param targetFormat Il formato di destinazione desiderato (es. "pdf", "docx")
     * @param outputFile   Il percorso completo dove salvare il file convertito localmente
     * @return Un oggetto ConversionResult che indica successo/fallimento e dettagli dell'operazione
     */
    public ConversionResult convertFile(File inputFile, String targetFormat, File outputFile, String conversionContextFile) {
        // Verifica preliminare della disponibilità del servizio
        if (!isServiceAvailable()) {
            return new ConversionResult(false, "Servizio di conversione non disponibile.");
        }



        try {
            // Costruisce l'URL per la conversione
            String url = baseUrl + "/api/converter/convert";

            // Prepara gli header HTTP per multipart/form-data
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // Costruisce il corpo della richiesta multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(inputFile)); // Aggiunge il file da convertire
            body.add("targetFormat", targetFormat); // Aggiunge il formato target desiderato
            File conversionContextConfig = new File(conversionContextFile);
            body.add("configFile", new FileSystemResource(conversionContextConfig)); //Aggiunge il file di configurazione della conversione

            // Crea l'entità HTTP completa con corpo e header
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Esegue la richiesta POST e si aspetta un array di byte come risposta
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Determina l'estensione corretta dal Content-Type della risposta
                String estensione = getExtensionFromMediaType(response.getHeaders().getContentType().toString());

                // Costruisce il nome del file finale con l'estensione corretta
                File renamedFile = new File(outputFile.getParent(), Utility.getBaseName(outputFile) + estensione);

                // Scrive i byte ricevuti nel file di output specificato
                try (FileOutputStream fos = new FileOutputStream(renamedFile)) {
                    fos.write(response.getBody());
                }

                // Ritorna il risultato di successo con il file generato
                return new ConversionResult(true, "File convertito e salvato con successo: " + outputFile.getName(), null, renamedFile);
            } else {
                // Gestisce risposte HTTP non di successo
                String errorMessage = "Errore durante la conversione: " + response.getStatusCode();
                logger.error(errorMessage);
                return new ConversionResult(false, errorMessage);
            }

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Gestisce errori HTTP specifici (4xx, 5xx)
            return recordError("Errore del server (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            // Gestisce errori di rete o connessione
            return recordError("Errore di connessione al servizio: " + e.getMessage());
        } catch (IOException e) {
            // Gestisce errori nella scrittura del file locale
            return recordError("Errore I/O durante il salvataggio del file convertito: " + e.getMessage());
        } catch (Exception e) {
            // Gestisce qualsiasi altro errore inatteso
            return recordError("Errore inatteso durante la conversione: " + e.getMessage());
        }
    }

    /**
     * Determina l'estensione del file dal MediaType utilizzando Apache Tika.
     *
     * @param mediaType Il tipo MIME del file (es. "application/pdf")
     * @return L'estensione corrispondente (es. ".pdf") o ".bin" come fallback
     */
    private String getExtensionFromMediaType(String mediaType) {
        try {
            //Mediatype relativo a docx protetto, ma non permette di riconoscere l'estensione
            if (mediaType.equals("application/x-tika-ooxml-protected"))
                return ".docx";
            // Utilizza Apache Tika per mappare il MIME type all'estensione
            MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
            MimeType mimeType = allTypes.forName(mediaType);
            return mimeType.getExtension();
        } catch (Exception e) {
            // Ritorna un'estensione generica in caso di errore
            return ".bin";
        }
    }

    /**
     * Metodo di utility per registrare errori nel log e creare un ConversionResult di fallimento.
     *
     * @param message Il messaggio di errore da registrare
     * @return Un ConversionResult che indica il fallimento con il messaggio specificato
     */
    private ConversionResult recordError(String message) {
        logger.error(message);
        return new ConversionResult(false, message);
    }


}