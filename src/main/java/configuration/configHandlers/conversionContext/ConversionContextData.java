package configuration.configHandlers.conversionContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configExceptions.JsonStructureException;
import configuration.jsonUtilities.JsonReader;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ConversionContextData implements JsonReader {
    private static final File jsonFile = new File("src/main/java/configuration/configFiles/conversionContext.json");
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>();
    private static final HashMap<String, Object> baseTemplate = readData();
    protected static final ThreadLocal<HashMap<String, Object>> context =
            ThreadLocal.withInitial(() -> new HashMap<>(baseTemplate));

    private static HashMap<String, Object> readData() throws JsonStructureException {
        return JsonReader.read(new TypeReference<HashMap<String, Object>>() {}, "data", jsonFile, rootReference);
    }
}
