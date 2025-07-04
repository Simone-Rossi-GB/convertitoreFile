package webService.client.configuration.configHandlers.conversionContext;

import webService.client.configuration.jsonUtilities.JsonUtility;
import webService.client.configuration.jsonUtilities.recognisedWrappers.RecognisedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ConversionContextInstance {
    /** Logger utilizzato per messaggi di debug e tracciamento errori. */
    private static final Logger logger = LogManager.getLogger(ConversionContextInstance.class);

    /** Riferimento al file JSON da cui leggere i parametri di configurazione. */
    protected final File jsonFile;

    public ConversionContextInstance(File jsonFile) {
        // Elenco dei campi obbligatori che devono essere presenti nel file di configurazione
        List<String> MANDATORY_FIELDS = Arrays.asList(
                "destinationFormat",
                "password",
                "protectedOutput",
                "union",
                "zippedOutput"
        );
        this.jsonFile = jsonFile;
        // Esegue la validazione della struttura del JSON
        JsonUtility.validateJsonFromStringOrFile(new RecognisedFile(jsonFile), MANDATORY_FIELDS);
        logger.info("{} validato correttamente", jsonFile.getPath());
    }

    public File getJsonFile() {
        return jsonFile;
    }
}
