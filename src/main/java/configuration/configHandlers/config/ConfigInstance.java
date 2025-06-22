package configuration.configHandlers.config;

import configuration.configExceptions.NullConfigValueException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigInstance {
    private static final Logger logger = LogManager.getLogger(ConfigInstance.class);

    // Percorso del file di configurazione da cui vengono lette tutte le impostazioni
    protected final File jsonFile;

    private String successOutputDir;
    private String errorOutputDir;
    private String monitoredDir;
    private Boolean monitorAtStart;
    private Map<String, Map<String, String>> conversions;

    public ConfigInstance(File jsonFile) {
        List<String> nullFields = new ArrayList<>();
        this.jsonFile = jsonFile;

        InstanceConfigReader jsonReader = new InstanceConfigReader(jsonFile);

        successOutputDir = assignOrTrackNull(jsonReader.readSuccessOutputDir(), "successOutputDir", nullFields);
        errorOutputDir   = assignOrTrackNull(jsonReader.readErrorOutputDir(), "errorOutputDir", nullFields);
        monitoredDir     = assignOrTrackNull(jsonReader.readMonitoredDir(), "monitoredDir", nullFields);
        monitorAtStart   = assignOrTrackNull(jsonReader.readIsMonitoringEnabledAtStart(), "monitorAtStart", nullFields);
        conversions      = assignOrTrackNull(jsonReader.readConversions(), "conversions", nullFields);

        if (!nullFields.isEmpty()) {
            logger.error("Valori nulli nella configurazione: {}", String.join(", ", nullFields));
            throw new NullConfigValueException("Valori nulli nella configurazione: " + String.join(", ", nullFields));
        }
    }

    private <T> T assignOrTrackNull(T value, String name, List<String> nullFields) {
        if (value == null) nullFields.add(name);
        return value;
    }

    public File getJsonFile(){
        return jsonFile;
    }

    public String getSuccessOutputDir() {
        return successOutputDir;
    }

    public String getErrorOutputDir() {
        return errorOutputDir;
    }

    public String getMonitoredDir() {
        return monitoredDir;
    }

    public Map<String, Map<String, String>> getConversions() {
        return conversions;
    }

    public boolean getMonitorAtStart() {
        return monitorAtStart;
    }
}
