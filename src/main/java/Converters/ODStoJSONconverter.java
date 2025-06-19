package Converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;
import converter.ConvertionException;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class ODStoJSONconverter implements Converter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ArrayList<File> convert(File srcFile) throws Exception, ConvertionException {
        if (controlloFileNonVuoto(srcFile)){
            return convertInternal(srcFile, null, false);
        }

        throw new ConvertionException("File vuoto o corrotto");
    }

    @Override
    public ArrayList<File> convert(File srcFile, String password) throws ConvertionException, Exception {
        if (controlloFileNonVuoto(srcFile)){
            return convertInternal(srcFile, password, false);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    @Override
    public ArrayList<File> convert(File srcFile, boolean opzioni) throws Exception {
        if (controlloFileNonVuoto(srcFile)) {
            return convertInternal(srcFile, null, opzioni);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    @Override
    public ArrayList<File> convert(File srcFile, String password, boolean opzioni) throws Exception {
        if (controlloFileNonVuoto(srcFile)) {
            return convertInternal(srcFile, password, opzioni);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    /**
     * Controlla se il file è vuoto (0 byte).
     * @param srcFile Il file da controllare.
     * @return true se il file NON è vuoto, false se è vuoto.
     */
    private boolean controlloFileNonVuoto(File srcFile) {
        return srcFile != null && srcFile.length() > 0;
    }

    private ArrayList<File> convertInternal(File odsFile, String password, boolean opzioni) throws Exception {
        List<Map<String, Object>> jsonData = new ArrayList<>();

        SpreadsheetDocument spreadsheet = SpreadsheetDocument.loadDocument(odsFile);
        Table table = spreadsheet.getSheetByIndex(0);

        List<String> headers = new ArrayList<>();
        boolean headerParsed = false;
        int columnCount = table.getColumnCount();

        for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
            Row row = table.getRowByIndex(rowIndex);
            if (row == null) continue;

            if (!headerParsed) {
                for (int col = 0; col < columnCount; col++) {
                    Cell cell = row.getCellByIndex(col);
                    headers.add(cell.getDisplayText().trim());
                }
                headerParsed = true;
                continue;
            }

            Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = row.getCellByIndex(col);
                rowMap.put(headers.get(col), cell.getDisplayText().trim());
            }

            if (!rowMap.isEmpty()) {
                jsonData.add(rowMap);
            }
        }

        File outFile = File.createTempFile("converted-", ".json");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fos, jsonData);
        }

        ArrayList<File> output = new ArrayList<>();
        output.add(outFile);
        return output;
    }
}
