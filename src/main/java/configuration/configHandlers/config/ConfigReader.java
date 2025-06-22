package configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.atomic.AtomicReference;

public class ConfigReader extends BaseConfigReader {
    // Contenitore mutabile per il nodo radice del file JSON; aggiornabile tramite metodi statici
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>(null);
    private static final ConfigReader INSTANCE = new ConfigReader();

    private ConfigReader() {
        super(rootReference);
    }
    public static ConfigReader getSingleton(){
        return INSTANCE;
    }
}
