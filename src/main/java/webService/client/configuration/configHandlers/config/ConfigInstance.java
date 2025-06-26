package webService.client.configuration.configHandlers.config;

import webService.client.configuration.jsonUtilities.JsonUtility;
import webService.client.configuration.jsonUtilities.recognisedWrappers.RecognisedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Rappresenta un'istanza di configurazione associata a un file JSON.
 * <p>
 * Durante la creazione, esegue una validazione strutturale sul contenuto del file,
 * assicurandosi che tutti i campi richiesti siano presenti.
 */
public class ConfigInstance {

    /** Logger utilizzato per messaggi di debug e tracciamento errori. */
    private static final Logger logger = LogManager.getLogger(ConfigInstance.class);

    /** Riferimento al file JSON da cui leggere i parametri di configurazione. */
    protected final File jsonFile;

    /**
     * Costruttore che inizializza l'istanza di configurazione e valida il contenuto del file JSON.
     *
     * @param jsonFile file di configurazione da validare
     * @throws webService.client.configuration.configExceptions.JsonStructureException se il file non contiene tutti i campi obbligatori
     */
    public ConfigInstance(File jsonFile) {
        // Elenco dei campi obbligatori che devono essere presenti nel file di configurazione
        List<String> MANDATORY_FIELDS = Arrays.asList(
                "successOutputDir",
                "errorOutputDir",
                "monitoredDir",
                "monitorAtStart",
                "conversions"
        );

        this.jsonFile = jsonFile;
        // Esegue la validazione della struttura del JSON
        JsonUtility.validateJsonFromStringOrFile(new RecognisedFile(jsonFile), MANDATORY_FIELDS);
        logger.info("{} validato correttamente", jsonFile.getPath());
    }

    /**
     * Restituisce il file JSON attualmente associato a questa istanza di configurazione.
     *
     * @return il file di configurazione
     */
    public File getJsonFile() {
        return jsonFile;
    }
}
