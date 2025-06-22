package configuration.configHandlers.config;

import configuration.configUtilities.JsonWriter;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class InstanceConfigWriter extends BaseConfigWriter implements JsonWriter {
    private File jsonFile;

    public InstanceConfigWriter(File jsonFile) {
        super(jsonFile, new AtomicReference<>(null));
    }
}
