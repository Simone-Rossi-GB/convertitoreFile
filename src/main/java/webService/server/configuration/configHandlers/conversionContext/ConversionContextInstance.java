package webService.server.configuration.configHandlers.conversionContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import webService.server.configuration.jsonUtilities.JsonUtility;
import webService.server.configuration.jsonUtilities.recognisedWrappers.RecognisedFile;

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
                "protected",
                "union",
                "zippedOutput"
        );
        this.jsonFile = new File(jsonFile.getAbsolutePath());
        // Esegue la validazione della struttura del JSON
        JsonUtility.validateJsonFromStringOrFile(new RecognisedFile(this.jsonFile), MANDATORY_FIELDS);
        logger.info("{} validato correttamente", this.jsonFile.getPath());
    }

    public File getJsonFile() {
        return jsonFile;
    }
}
