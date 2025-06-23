package configuration.jsonUtilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configExceptions.JsonStructureException;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public interface JsonData {
    static HashMap<String, Object> readData(File jsonFile, AtomicReference<ObjectNode> rootReference) throws JsonStructureException {
        return JsonReader.read(new TypeReference<HashMap<String, Object>>() {}, "data", jsonFile, rootReference);
    }
}
