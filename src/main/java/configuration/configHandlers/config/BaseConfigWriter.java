package configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configUtilities.JsonWriter;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class BaseConfigWriter extends ConfigData {
    // Contenitore mutabile per il nodo radice del file JSON; aggiornabile tramite metodi statici
    protected final AtomicReference<ObjectNode> rootReference;

    protected BaseConfigWriter(AtomicReference<ObjectNode> rootReference) {
        this.rootReference = rootReference;
    }
    protected BaseConfigWriter(File newJsonFile, AtomicReference<ObjectNode> rootReference) {
        jsonFile = newJsonFile;
        this.rootReference = rootReference;
    }

    public void writeSuccessOutputDir(String newSuccessOutputDir){
        JsonWriter.write(newSuccessOutputDir, "successOutputDir", jsonFile, rootReference);
    }
    public void writeErrorOutputDir(String newErrorOutputDir){
        JsonWriter.write(newErrorOutputDir, "errorOutputDir", jsonFile, rootReference);
    }
    public void writeMonitoredDir(String newMonitoredDir){
        JsonWriter.write(newMonitoredDir, "monitoredOutputDir", jsonFile, rootReference);
    }
    public void writeIsMonitoringEnabledAtStart(boolean isMonitoringEnabledAtStart){
        JsonWriter.write(isMonitoringEnabledAtStart, "monitorAtStart", jsonFile, rootReference);
    }
}
