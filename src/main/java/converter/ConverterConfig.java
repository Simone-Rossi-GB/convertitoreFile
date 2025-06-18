package converter;

import java.util.Map;

public class ConverterConfig {
    private String successOutputDir;
    private String errorOutputDir;
    private String monitoredDir;
    private boolean monitorAtStart;

    private Map<String, Map<String, String>> conversions;

    public String getSuccessOutputDir() {
        return successOutputDir;
    }

    public String getErrorOutputDir() {
        return errorOutputDir;
    }

    public String getMonitoredDir() {
        return monitoredDir;
    }

    public Map<String, Map<String, String>> getConversions() {
        return conversions;
    }

    public boolean getMonitorAtStart() {
        return monitorAtStart;
    }
}
