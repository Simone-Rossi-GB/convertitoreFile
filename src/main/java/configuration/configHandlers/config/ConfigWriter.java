package configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.atomic.AtomicReference;

public class ConfigWriter extends BaseConfigWriter {
    // Contenitore mutabile per il nodo radice del file JSON; aggiornabile tramite metodi statici
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>(null);
    private static final ConfigWriter INSTANCE = new ConfigWriter();

    private ConfigWriter() {
        super(rootReference);
    }
    public static ConfigWriter getSingleton(){
        return INSTANCE;
    }

}
