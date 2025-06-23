package configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.jsonUtilities.JsonWriter;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class InstanceConfigWriter extends ConfigData {
    private final AtomicReference<ObjectNode> rootReference = new AtomicReference<>(null);
    private final File jsonFile;

    public InstanceConfigWriter(File jsonFile) {
        this.jsonFile = jsonFile;
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

    public void writeIsMultipleConversionEnabled(boolean isMonitoringEnabledAtStart){
        JsonWriter.write(isMonitoringEnabledAtStart, "multipleConversion", jsonFile, rootReference);
    }
}
