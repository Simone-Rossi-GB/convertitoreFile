import java.util.Map;

public class ConverterConfig {
    private String successOutputDir;
    private String errorOutputDir;
    private Map<String, Map<String, String>> conversions;

    public String getSuccessOutputDir() {
        return successOutputDir;
    }

    public String getErrorOutputDir() {
        return errorOutputDir;
    }

    public Map<String, Map<String, String>> getConversions() {
        return conversions;
    }
}
