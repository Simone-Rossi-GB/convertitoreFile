package webService.client.gui.jsonHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private static final File CONFIG_FILE = new File("src/main/java/webService/client/gui/jsonHandler/guiConfig.json");

    public static JsonConfig readConfig() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(CONFIG_FILE, JsonConfig.class);
        } catch (IOException e) {
            System.err.println("ERRORE: Impossibile leggere il file di configurazione");
            e.printStackTrace();
            return null;
        }
    }

    public static void writeConfig(JsonConfig config) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(CONFIG_FILE, config);
        } catch (IOException e) {
            System.err.println("ERRORE: Impossibile scrivere il file di configurazione");
            e.printStackTrace();
        }
    }
}
