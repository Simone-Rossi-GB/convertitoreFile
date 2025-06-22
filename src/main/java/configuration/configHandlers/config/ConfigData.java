package configuration.configHandlers.config;

import java.io.File;

public abstract class ConfigData {
    protected static File jsonFile;

    public static void update(ConfigInstance configInstance){
        jsonFile = configInstance.getJsonFile();
    }

    public static File getJsonFile() {
        return jsonFile;
    }
}
