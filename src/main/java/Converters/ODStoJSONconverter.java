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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


/**
 * Classe responsabile della conversione di file ODS (OpenDocument Spreadsheet)
 * in formato JSON.
 */
public class ODStoJSONconverter implements Converter {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converte un file ODS in JSON.
     *
     * @param srcFile File ODS da convertire
     * @return Lista contenente il file JSON generato
     * @throws Exception           In caso di errori generici
     * @throws ConvertionException Se il file è nullo, vuoto o non valido
     */
    @Override
    public ArrayList<File> convert(File srcFile) throws Exception, ConvertionException {
        if (controlloFileNonVuoto(srcFile)) {
            return convertInternal(srcFile);
        }
        throw new ConvertionException("L'oggetto srcFile non esiste o è vuoto.");
    }


    /**
     * Verifica che il file esista e non sia vuoto.
     *
     * @param srcFile Il file da controllare
     * @return true se il file esiste ed è non vuoto, false altrimenti
     */
    private boolean controlloFileNonVuoto(File srcFile) {
        return srcFile != null && srcFile.exists() && srcFile.length() > 0;
    }

    /**
     * Metodo interno che esegue la logica di conversione da ODS a JSON.
     *
     * @param odsFile  Il file sorgente ODS

     * @return Lista contenente un singolo file JSON generato
     * @throws Exception In caso di errori nella lettura o scrittura file
     */
    private ArrayList<File> convertInternal(File odsFile) throws Exception {
        if (odsFile == null) {
            throw new IllegalArgumentException("L'oggetto odsFile non esiste.");
        }

        List<Map<String, Object>> jsonData = new ArrayList<>();
        SpreadsheetDocument spreadsheet = SpreadsheetDocument.loadDocument(odsFile);
        Table table = spreadsheet.getSheetByIndex(0);
        if (table == null) {
            throw new ConvertionException("L'oggetto table non esiste nel file ODS.");
        }

        List<String> headers = new ArrayList<>();
        boolean headerParsed = false;
        int columnCount = table.getColumnCount();

        for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
            Row row = table.getRowByIndex(rowIndex);
            if (row == null) continue;

            if (!headerParsed) {
                for (int col = 0; col < columnCount; col++) {
                    Cell cell = row.getCellByIndex(col);
                    String header = (cell != null) ? cell.getDisplayText().trim() : "Colonna" + col;
                    headers.add(header.isEmpty() ? "Colonna" + col : header);
                }
                headerParsed = true;
                continue;
            }

            Map<String, Object> rowMap = new LinkedHashMap<>();
            boolean isEmptyRow = true;

            for (int col = 0; col < headers.size(); col++) {
                Cell cell = row.getCellByIndex(col);
                String value = (cell != null) ? cell.getDisplayText().trim() : "";
                rowMap.put(headers.get(col), value);
                if (!value.isEmpty()) {
                    isEmptyRow = false;
                }
            }

            if (!isEmptyRow) {
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