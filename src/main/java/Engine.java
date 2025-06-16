import Converters.Converter;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
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

    public void conversione (String srcExt, String outExt, File srcFile) throws Exception{
        Map<String, Map<String, String>> conversions = config.getConversions();
        if(!conversions.containsKey(srcExt))
        throw new Exception("Conversione non supportata");
        Map<String, String> possibleConversions = conversions.get(srcExt);
        if(possibleConversions.containsKey(outExt)){
            String converterClassName = possibleConversions.get(outExt);
            System.out.println(srcExt + " " + outExt + " " + converterClassName);
            try {
                Class<?> clazz = Class.forName(converterClassName);
                Converter converter = (Converter) clazz.getDeclaredConstructor().newInstance();
                try{
                    List<File> outFiles = converter.convert(srcFile);
                    for(File f : outFiles)
                        spostaFile(config.getSuccessOutputDir(), f);
                } catch (IOException e) {
                    spostaFile(config.getErrorOutputDir(), srcFile);
                    throw new Exception("Errore nella conversione");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else
            throw new Exception("Conversione non supportata");
    }

    private void spostaFile(String outPath, File file) throws IOException {
        String fileName = file.getName();
        Path srcPath = Paths.get(file.getAbsolutePath());
        Path destPath = Paths.get(outPath + fileName);
        Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
    }
}