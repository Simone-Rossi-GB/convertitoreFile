package configuration.configHandlers.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configExceptions.JsonStructureException;
import configuration.jsonUtilities.JsonReader;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ConfigReader extends ConfigData {
    public static List<String> getFormatsWithAlphaChannel() throws JsonStructureException {
        return (List<String>) configDataMap.get("formatsWithAlphaChannel");
    }

    public static List<String> getFormatsRequiringIntermediateConversion() throws JsonStructureException {
        return (List<String>) configDataMap.get("formatsRequiringIntermediateConversion");
    }

    public static Map<String, Map<String, String>> getConversions() throws JsonStructureException {
        return (Map<String, Map<String, String>>) configDataMap.get("conversions");
    }

    public static List<String> getMandatoryEntries() throws JsonStructureException {
        return (List<String>) configDataMap.get("mandatoryEntries");
    }

    public static String getSuccessOutputDir() throws JsonStructureException {
        return configDataMap.get("successOutputDir").toString();
    }

    public static String getErrorOutputDir() throws JsonStructureException {
        return configDataMap.get("errorOutputDir").toString();
    }

    public static String getMonitoredDir() throws JsonStructureException {
        return configDataMap.get("monitoredDir").toString();
    }

    public static Boolean getIsMonitoringEnabledAtStart() throws JsonStructureException {
        return (Boolean) configDataMap.get("monitorAtStart");
    }

    public static Boolean getIsMultipleConversionEnabled() throws JsonStructureException {
        return (Boolean) configDataMap.get("multipleConversion");
    }

}
