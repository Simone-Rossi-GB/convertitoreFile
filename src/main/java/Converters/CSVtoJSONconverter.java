package Converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import converter.Log;
import converter.Utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CSVtoJSONconverter implements Converter{
    @Override
    public ArrayList<File> convert(File srcFile) throws IOException {
        Log.addMessage("Inizio conversione csv: "+Utility.estraiNomePiuEstensioneFile(srcFile)+" -> .json");
        BufferedReader reader = new BufferedReader(new FileReader(srcFile));
        String[] headers = reader.readLine().split(",");

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode jsonArray = objectMapper.createArrayNode();

        String line;
        while ((line = reader.readLine()) != null){
            String[] values = line.split(",");
            ObjectNode jsonObject = objectMapper.createObjectNode();
            for (int i=0; i < headers.length; i++){
                String cleanKey = headers[i].replaceAll("^\"|\"$","");
                String value  = values[i];
                if (values[i].matches("^-?\\d+(\\.\\d+)?$")){
                    jsonObject.put(cleanKey, Double.parseDouble(value));
                } else {
                    jsonObject.put(cleanKey, value.replaceAll("^\"|\"$",""));
                }
            }
            jsonArray.add(jsonObject);
        }
        reader.close();

        File outputFile = new File(srcFile.getName().split("\\.")[0]+".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, jsonArray);
        Log.addMessage("Creazione file .json completata: "+ Utility.estraiNomePiuEstensioneFile(outputFile));
        ArrayList<File> files = new ArrayList<>();
        files.add(outputFile);
        return files;
    }
}
