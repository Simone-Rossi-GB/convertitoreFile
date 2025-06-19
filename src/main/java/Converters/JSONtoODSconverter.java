package Converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import converter.ConvertionException;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Table;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class JSONtoODSconverter implements Converter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ArrayList<File> convert(File srcFile) throws Exception, ConvertionException {
        if(controlloFileNonVuoto(srcFile)){
            return convertInternal(srcFile, null, false);
        }
        throw new ConvertionException("File vuoto o corrrotto");
    }

    @Override
    public ArrayList<File> convert(File srcFile, String password) throws Exception, ConvertionException {
        if(controlloFileNonVuoto(srcFile)) {
            return convertInternal(srcFile, password, false);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    @Override
    public ArrayList<File> convert(File srcFile, boolean opzioni) throws Exception, ConvertionException {
        if(controlloFileNonVuoto(srcFile)){
            return convertInternal(srcFile, null, opzioni);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    @Override
    public ArrayList<File> convert(File srcFile, String password, boolean opzioni) throws Exception, ConvertionException {
        if(controlloFileNonVuoto(srcFile)){
            return convertInternal(srcFile, password, opzioni);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }


    /**
     * Controlla se il file è vuoto.
     * @param srcFile Il file da verificare.
     * @return true se il file NON è vuoto, false se è vuoto o nullo.
     */
    private boolean controlloFileNonVuoto(File srcFile) {
        return srcFile != null && srcFile.length() > 0;
    }







    private ArrayList<File> convertInternal(File jsonFile, String password, boolean opzioni) throws Exception {
        // Parsing del file JSON
        List<LinkedHashMap<String, Object>> data = objectMapper.readValue(
                jsonFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, LinkedHashMap.class)
        );

        if (data.isEmpty()) {
            throw new IllegalArgumentException("Il file JSON è vuoto o malformato.");
        }

        // Creazione nuovo documento .ods
        File outFile;
        try (SpreadsheetDocument document = SpreadsheetDocument.newSpreadsheetDocument()) {
            Table sheet = document.getSheetByIndex(0);
            sheet.setTableName("Dati");

            // Intestazioni
            Set<String> headers = data.get(0).keySet();
            int colIndex = 0;
            for (String header : headers) {
                sheet.getCellByPosition(colIndex++, 0).setStringValue(header);
            }

            // Dati
            int rowIndex = 1;
            for (Map<String, Object> row : data) {
                colIndex = 0;
                for (String header : headers) {
                    Object value = row.get(header);
                    sheet.getCellByPosition(colIndex++, rowIndex).setStringValue(value != null ? value.toString() : "");
                }
                rowIndex++;
            }

            // Scrittura su file temporaneo .ods
            outFile = File.createTempFile("converted-", ".ods");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                document.save(fos);
            }
        }

        ArrayList<File> output = new ArrayList<>();
        output.add(outFile);
        return output;
    }
}

