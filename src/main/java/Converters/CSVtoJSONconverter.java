package Converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVtoJSONconverter implements Converter{
    @Override
    public ArrayList<File> convert(File srcFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(srcFile));
        String[] headers = reader.readLine().split(",");

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode jsonArray = objectMapper.createArrayNode();

        String line;
        while ((line = reader.readLine()) != null){
            String[] values = line.split(",");
            ObjectNode jsonObject = objectMapper.createObjectNode();
            for (int i=0; i < headers.length; i++){
                jsonObject.put(headers[i], values[i]);
            }
            jsonArray.add(jsonObject);
        }
        reader.close();

        File outputFile = new File(srcFile.getName().split("\\.")[0]+".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, jsonArray);
        System.out.println("Conversione completata");
        ArrayList<File> files = new ArrayList<>();
        files.add(outputFile);
        return files;
    }
}
