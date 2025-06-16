import Converters.Converter;
import com.google.gson.Gson;

import java.io.FileReader;
import java.util.Map;

public class Engine{
    private ConverterConfig config = null;

    public Engine(){
        try (FileReader reader = new FileReader("C:\\Users\\DELLMuletto\\IdeaProjects\\convertitoreFile\\config\\config.json")) {
            Gson gson = new Gson();
            config = gson.fromJson(reader, ConverterConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Errore nella lettura del file di configurazione", e);
        }
    }

    public boolean conversione (String srcExt, String outExt){
        Map<String, Map<String, String>> conversions = config.getConversions();
        if(!conversions.containsKey(srcExt))
            return false;
        Map<String, String> possibleConversions = conversions.get(srcExt);
        if(possibleConversions.containsKey(outExt)){
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " " + outExt + " " + converterClassName);
            try {
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        else
            return false;
    }

}