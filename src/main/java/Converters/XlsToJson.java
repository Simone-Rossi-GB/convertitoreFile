package Converters;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

/**
 * Convertitore da file Excel (.xls) a JSON
 * Compatibile con Java 1.8
 */
public class XlsToJsonConverter {

    public static void main(String[] args) {
        // Configurazione file
        String inputFile = "input.xls";
        String outputFile = "output.json";

        // Parsing parametri da riga di comando
        if (args.length >= 2) {
            inputFile = args[0];
            outputFile = args[1];
        }

        XlsToJsonConverter converter = new XlsToJsonConverter();
        try {
            converter.convertXlsToJson(inputFile, outputFile);
        } catch (IOException e) {
            System.err.println("Errore I/O durante la conversione: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Errore durante la conversione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Converte un file XLS in formato JSON
     */
    public void convertXlsToJson(String inputFilePath, String outputFilePath) throws IOException {
        System.out.println("Inizio conversione: " + inputFilePath + " -> " + outputFilePath);

        try (InputStream inputStream = new FileInputStream(inputFilePath);
             Workbook workbook = new HSSFWorkbook(inputStream)) {

            // Prende il primo foglio
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("Elaborazione foglio: " + sheet.getSheetName());

            // Converte i dati
            List<Map<String, Object>> jsonData = convertSheetToJson(sheet);

            // Scrive il file JSON
            writeJsonToFile(jsonData, outputFilePath);

            System.out.println("Conversione completata! Righe elaborate: " + jsonData.size());
        }
    }

    /**
     * Converte un foglio Excel in una lista di oggetti JSON
     */
    private List<Map<String, Object>> convertSheetToJson(Sheet sheet) {
        List<Map<String, Object>> jsonData = new ArrayList<>();
        Iterator<Row> rowIterator = sheet.iterator();
        List<String> headers = new ArrayList<>();

        // Legge le intestazioni (prima riga)
        if (rowIterator.hasNext()) {
            Row headerRow = rowIterator.next();
            for (Cell cell : headerRow) {
                String header = getCellValueAsString(cell).trim();
                headers.add(header.isEmpty() ? "column_" + cell.getColumnIndex() : header);
            }
            System.out.println("Intestazioni trovate: " + headers);
        }

        // Elabora le righe di dati
        int rowCount = 0;
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();

            // Salta righe completamente vuote
            if (isRowEmpty(row)) {
                continue;
            }

            Map<String, Object> rowData = new LinkedHashMap<>();

            for (int i = 0; i < headers.size(); i++) {
                Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                Object cellValue = getCellValue(cell);
                rowData.put(headers.get(i), cellValue);
            }

            jsonData.add(rowData);
            rowCount++;
        }

        System.out.println("Righe di dati elaborate: " + rowCount);
        return jsonData;
    }

    /**
     * Estrae il valore di una cella come Object (preserva i tipi)
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();

            case Cell.CELL_TYPE_NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    double numValue = cell.getNumericCellValue();
                    // Se è un numero intero, restituisce come Integer
                    if (numValue == Math.floor(numValue)) {
                        return (int) numValue;
                    }
                    return numValue;
                }

            case Cell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue();

            case Cell.CELL_TYPE_FORMULA:
                // Tenta di valutare la formula
                try {
                    return evaluateFormula(cell);
                } catch (Exception e) {
                    return cell.getCellFormula();
                }

            case Cell.CELL_TYPE_BLANK:
                return null;

            default:
                return null;
        }
    }

    /**
     * Estrae il valore di una cella come String
     */
    private String getCellValueAsString(Cell cell) {
        Object value = getCellValue(cell);
        return value != null ? value.toString() : "";
    }

    /**
     * Valuta una formula e restituisce il risultato
     */
    private Object evaluateFormula(Cell cell) {
        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        CellValue cellValue = evaluator.evaluate(cell);

        switch (cellValue.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC:
                double numValue = cellValue.getNumberValue();
                // Se è un numero intero, restituisce come Integer
                if (numValue == Math.floor(numValue)) {
                    return (int) numValue;
                }
                return numValue;
            case Cell.CELL_TYPE_STRING:
                return cellValue.getStringValue();
            case Cell.CELL_TYPE_BOOLEAN:
                return cellValue.getBooleanValue();
            default:
                return cell.getCellFormula();
        }
    }

    /**
     * Verifica se una riga è completamente vuota
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != Cell.CELL_TYPE_BLANK) {
                String cellValue = getCellValueAsString(cell).trim();
                if (!cellValue.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Scrive i dati JSON su file
     */
    private void writeJsonToFile(List<Map<String, Object>> data, String filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Configura il mapper per una formattazione leggibile
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), data);
        } catch (JsonMappingException e) {
            throw new IOException();
        }
    }
}