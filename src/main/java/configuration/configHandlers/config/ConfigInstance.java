package configuration.configHandlers.config;

import configuration.configExceptions.NullConfigValueException;
import configuration.jsonUtilities.JsonUtility;
import configuration.jsonUtilities.RecognisedWrappers.RecognisedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ConfigInstance {
    private static final Logger logger = LogManager.getLogger(ConfigInstance.class);

    // Percorso del file di configurazione da cui vengono lette tutte le impostazioni
    protected final File jsonFile;

    public ConfigInstance(File jsonFile) {
        List<String> MANDATORY_FIELDS = Arrays.asList("successOutputDir", "errorOutputDir", "monitoredDir", "monitorAtStart", "conversions");
        List<String> nullFields = new ArrayList<>();
        this.jsonFile = jsonFile;

        JsonUtility.validateJsonFromStringOrFile(new RecognisedFile(jsonFile), MANDATORY_FIELDS);
    }

    public File getJsonFile(){
        return jsonFile;
    }
}
