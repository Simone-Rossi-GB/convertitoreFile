package configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configUtilities.JsonWriter;

import java.util.concurrent.atomic.AtomicReference;

public class ConfigWriter extends ConfigData implements JsonWriter {
    // Contenitore mutabile per il nodo radice del file JSON; aggiornabile tramite metodi statici
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>(null);

    public static void writeSuccessOutputDir(String newSuccessOutputDir){
        JsonWriter.write(newSuccessOutputDir, "successOutputDir", jsonFile, rootReference);
    }
}
