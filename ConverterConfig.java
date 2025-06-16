import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public class ConverterConfig {
    private String folderPath = "config/";
    private String configFile = "config.json";
    private String configFilePath = "";
    private Gson gson;
    private Type mapType;

    public ConverterConfig() {
        this.folderPath = folderPath;
        this.configFile = configFile;
        this.configFilePath = folderPath + configFile;

        try {
            fileReader = new FileReader(configFilePath);
            gson = new Gson();
            mapType = new TypeToken<HashMap<String, LinkedList<String>>>() {}.getType();

            Map<String, String> configMap = gson.fromJson(fileReader, mapType);

            for(String key : mapType.getKeys())

        } catch (Exception e ) {

        }
    }


}
