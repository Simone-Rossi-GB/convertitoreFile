package configuration.configHandlers.conversionContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configExceptions.JsonStructureException;
import configuration.jsonUtilities.JsonData;
import configuration.jsonUtilities.JsonReader;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public abstract class ConversionContextData implements JsonData {
    private static final File jsonFile = new File("src/main/java/configuration/configFiles/conversionContext.json");
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>();

    private static final HashMap<String, Object> baseTemplate = JsonData.readData(jsonFile, rootReference);
    protected static final ThreadLocal<HashMap<String, Object>> context =
            ThreadLocal.withInitial(() -> new HashMap<>(baseTemplate));

}
