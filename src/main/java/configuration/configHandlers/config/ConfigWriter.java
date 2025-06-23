package configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.jsonUtilities.JsonWriter;

import java.util.concurrent.atomic.AtomicReference;

public class ConfigWriter extends ConfigData {
    // Contenitore mutabile per il nodo radice del file JSON; aggiornabile tramite metodi statici
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>(null);

    public void setSuccessOutputDir(String newSuccessOutputDir){
        configDataMap.put("successOutputDir", newSuccessOutputDir);
    }

    public void setErrorOutputDir(String newErrorOutputDir){
        configDataMap.put("errorOutputDir", newErrorOutputDir);
    }

    public void setMonitoredDir(String newMonitoredDir){
        configDataMap.put("monitoredDir", newMonitoredDir);
    }

    public void setIsMonitoringEnabledAtStart(boolean isMonitoringEnabledAtStart){
        configDataMap.put("monitorAtStart", isMonitoringEnabledAtStart);
    }

    public void setIsMultipleConversionEnabled(boolean isMultipleConversionEnabled){
        configDataMap.put("multipleConversion", isMultipleConversionEnabled);
    }
}
