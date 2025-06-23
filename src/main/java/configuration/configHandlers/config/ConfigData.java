package configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.jsonUtilities.JsonData;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ConfigData implements JsonData {
    private static File jsonFile;
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>();
    protected static HashMap<String, Object> configDataMap;

    public static void update(ConfigInstance configInstance){
        jsonFile = configInstance.getJsonFile();
        configDataMap = JsonData.readData(jsonFile, rootReference);
    }
    public static File getJsonFile() {
        return jsonFile;
    }
}
