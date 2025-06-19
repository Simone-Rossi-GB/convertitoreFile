package Converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import converter.ConvertionException;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Table;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.*;

public class JSONtoODSconverter implements Converter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ArrayList<File> convert(File srcFile) throws Exception, ConvertionException {
        if (controlloFileNonVuoto(srcFile)) {
            File validJsonFile = ensureJSONArrayFormat(srcFile);
            return convertInternal(validJsonFile, null, false);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    @Override
    public ArrayList<File> convert(File srcFile, String password) throws Exception, ConvertionException {
        if (controlloFileNonVuoto(srcFile)) {
            File validJsonFile = ensureJSONArrayFormat(srcFile);
            return convertInternal(validJsonFile, password, false);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    @Override
    public ArrayList<File> convert(File srcFile, boolean opzioni) throws Exception, ConvertionException {
        if (controlloFileNonVuoto(srcFile)) {
            File validJsonFile = ensureJSONArrayFormat(srcFile);
            return convertInternal(validJsonFile, null, opzioni);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    @Override
    public ArrayList<File> convert(File srcFile, String password, boolean opzioni) throws Exception, ConvertionException {
        if (controlloFileNonVuoto(srcFile)) {
            File validJsonFile = ensureJSONArrayFormat(srcFile);
            return convertInternal(validJsonFile, password, opzioni);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    /**
     * Controlla se il file è vuoto.
     */
    private boolean controlloFileNonVuoto(File srcFile) {
        return srcFile != null && srcFile.length() > 0;
    }

    /**
     * Controlla se il file JSON è un array. Se no, lo trasforma aggiungendo parentesi quadre.
     */
    private File ensureJSONArrayFormat(File jsonFile) throws Exception {
        String content = new String(Files.readAllBytes(jsonFile.toPath())).trim();

        boolean startsWithBracket = content.startsWith("[");
        boolean endsWithBracket = content.endsWith("]");

        if (!startsWithBracket || !endsWithBracket) {
            // Wrap manuale
            content = "[" + content;
            if (!endsWithBracket) {
                content += "]";
            }

            // Salva contenuto corretto in file temporaneo
            File fixedFile = File.createTempFile("fixed-json-", ".json");
            Files.write(fixedFile.toPath(), content.getBytes());
            return fixedFile;
        }

        // JSON già valido come array
        return jsonFile;
    }

    private ArrayList<File> convertInternal(File jsonFile, String password, boolean opzioni) throws Exception {
        List<LinkedHashMap<String, Object>> data = objectMapper.readValue(
                jsonFile,
                objectMapper.getTypeFactory().constructCollectionType(List.class, LinkedHashMap.class)
        );

        if (data.isEmpty()) {
            throw new IllegalArgumentException("Il file JSON è vuoto o malformato.");
        }

        File outFile;
        try (SpreadsheetDocument document = SpreadsheetDocument.newSpreadsheetDocument()) {
            Table sheet = document.getSheetByIndex(0);
            sheet.setTableName("Dati");

            // Header
            Set<String> headers = data.get(0).keySet();
            int colIndex = 0;
            for (String header : headers) {
                sheet.getCellByPosition(colIndex++, 0).setStringValue(header);
            }

            // Rows
            int rowIndex = 1;
            for (Map<String, Object> row : data) {
                colIndex = 0;
                for (String header : headers) {
                    Object value = row.get(header);
                    sheet.getCellByPosition(colIndex++, rowIndex).setStringValue(value != null ? value.toString() : "");
                }
                rowIndex++;
            }

            // Salvataggio su file temporaneo
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
