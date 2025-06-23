package configuration.configHandlers.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configExceptions.JsonStructureException;
import configuration.jsonUtilities.JsonReader;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseConfigReader extends ConfigData implements JsonReader {
    // Contenitore mutabile per il nodo radice del file JSON; aggiornabile tramite metodi statici
    protected final AtomicReference<ObjectNode> rootReference;

    protected BaseConfigReader(AtomicReference<ObjectNode> rootReference) {
        this.rootReference = rootReference;
    }
    protected BaseConfigReader(File newJsonFile, AtomicReference<ObjectNode> rootReference) {
        jsonFile = newJsonFile;
        this.rootReference = rootReference;
    }

    public List<String> readFormatsWithAlphaChannel() throws JsonStructureException {
        return JsonReader.read(new TypeReference<List<String>>() {}, "formatsWithAlphaChannel", jsonFile, rootReference);
    }

    public List<String> readFormatsRequiringIntermediateConversion() throws JsonStructureException {
        return JsonReader.read(new TypeReference<List<String>>() {}, "getFormatsRequiringIntermediateConversion", jsonFile, rootReference);
    }

    public Map<String, Map<String, String>> readConversions() throws JsonStructureException {
        return JsonReader.read(new TypeReference<Map<String, Map<String, String>>>() {}, "conversions", jsonFile, rootReference);
    }

    public List<String> readMandatoryEntries() throws JsonStructureException {
        return JsonReader.read(new TypeReference<List<String>>() {}, "mandatoryEntries", jsonFile, rootReference);
    }

    public String readSuccessOutputDir() throws JsonStructureException {
        return JsonReader.read(new TypeReference<String>() {}, "successOutputDir", jsonFile, rootReference);
    }

    public String readErrorOutputDir() throws JsonStructureException {
        return JsonReader.read(new TypeReference<String>() {}, "errorOutputDir", jsonFile, rootReference);
    }

    public String readMonitoredDir() throws JsonStructureException {
        return JsonReader.read(new TypeReference<String>() {}, "monitoredDir", jsonFile, rootReference);
    }

    public Boolean readIsMonitoringEnabledAtStart() throws JsonStructureException {
        return JsonReader.read(new TypeReference<Boolean>() {}, "monitorAtStart", jsonFile, rootReference);
    }
}
