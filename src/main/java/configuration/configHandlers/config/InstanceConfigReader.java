package configuration.configHandlers.config;

import configuration.jsonUtilities.JsonReader;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class InstanceConfigReader extends BaseConfigReader implements JsonReader {
    private File jsonFile;

    public InstanceConfigReader(File jsonFile) {
        super(jsonFile, new AtomicReference<>(null));
    }
}
