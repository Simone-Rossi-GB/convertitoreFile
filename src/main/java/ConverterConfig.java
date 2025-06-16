import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ConverterConfig {
    private final String folderPath = "config/";
    private final String configFile = "config.json";
    private String configFilePath = "";
    private Gson gson;
    private Type mapType;
    private static Map<String, LinkedList<String>> configMap;

    public ConverterConfig() {
        this.configFilePath = folderPath + configFile;

        try {
            FileReader fileReader = new FileReader(configFilePath);
            gson = new Gson();
            mapType = new TypeToken<HashMap<String, LinkedList<String>>>() {}.getType();

            configMap = gson.fromJson(fileReader, mapType);

            /*for(String key : configMap.keySet()) {
                System.out.print(key + ": ");
                for(String value : configMap.get(key)) {
                    System.out.print(value + " - ");
                }
                System.out.println();
            }


            uscirà una roba così
            pdf: doc - docx - ecc.
             */

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public static void selectConversion(String sourceFilePath){
        int lastDotIndex = sourceFilePath.lastIndexOf(".");
        String extension = "";
        if(lastDotIndex > 0 && lastDotIndex < sourceFilePath.length() - 1)
            extension = sourceFilePath.substring(lastDotIndex + 1);
        System.out.println(extension + ": ");
        for(String value : configMap.get(extension)) {
            System.out.print(value + " - ");
        }
        System.out.println();

    }

}
