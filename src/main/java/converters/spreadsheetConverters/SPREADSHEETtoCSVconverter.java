package converters.spreadsheetConverters;

import converters.Converter;
import converters.exception.ConversionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;

import java.io.*;
import java.nio.file.Files;

public class SPREADSHEETtoCSVconverter extends Converter {
    private static final Logger logger = LogManager.getLogger(SPREADSHEETtoCSVconverter.class);

    @Override
    public File convert(File spreadsheetFile) throws Exception {
        if (!spreadsheetFile.exists() || !spreadsheetFile.isFile()) {
            logger.error("Errore durante la conversione: il file non esiste o è corrotto.");
            throw new IllegalArgumentException("Il file non esiste o è vuoto.");
        }

        logger.info("Conversione iniziata con parametri:\n | inputFile.getPath() = {}", spreadsheetFile.getPath());

        File outputFile;
        try {
            outputFile = convertToCsv(spreadsheetFile);
            if (outputFile.exists()) {
                logger.info("File convertito con successo: {}", outputFile.getName());
            } else {
                logger.error("Conversione fallita: file CSV non creato correttamente");
            }
        } catch (Exception e) {
            logger.error("Errore durante la conversione: {}", e.getMessage());
            throw new ConversionException("Errore nella conversione Spreadsheet to CSV");
        }
        return outputFile;
    }

    private File convertToCsv(File spreadsheetFile) throws IOException {
        String fileName = spreadsheetFile.getName().toLowerCase();
        File csvFile = new File(spreadsheetFile.getParent(), fileName.replaceAll("\\.xlsx$|\\.xls$|\\.ods$", "") + ".csv");

        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            convertExcelToCsv(spreadsheetFile, csvFile);
        } else if (fileName.endsWith(".ods")) {
            convertOdsToCsv(spreadsheetFile, csvFile);
        } else {
            throw new IllegalArgumentException("Formato non supportato: " + fileName);
        }

        return csvFile;
    }

    private void convertExcelToCsv(File excelFile, File csvFile) throws IOException {
        try (InputStream inputStream = Files.newInputStream(excelFile.toPath());
             Workbook workbook = excelFile.getName().endsWith(".xlsx") ? new XSSFWorkbook(inputStream) : new HSSFWorkbook(inputStream);
             PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {

            Sheet sheet = workbook.getSheetAt(0); // Primo foglio
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                StringBuilder rowString = new StringBuilder();
                for (Cell cell : row) {
                    rowString.append(getCellValue(cell)).append(",");
                }
                writer.println(rowString.length() > 0 ? rowString.substring(0, rowString.length() - 1) : "");
            }
        }
    }

    private void convertOdsToCsv(File odsFile, File csvFile) throws IOException {
        try (SpreadsheetDocument spreadsheet = SpreadsheetDocument.loadDocument(odsFile);
             PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {

            Table table = spreadsheet.getSheetByIndex(0); // Primo foglio
            for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
                Row row = table.getRowByIndex(rowIndex);
                StringBuilder rowString = new StringBuilder();
                for (int colIndex = 0; colIndex < row.getCellCount(); colIndex++) {
                    rowString.append(row.getCellByIndex(colIndex).getDisplayText().trim()).append(",");
                }
                writer.println(rowString.length() > 0 ? rowString.substring(0, rowString.length() - 1) : "");
            }
        } catch (Exception e) {
            throw new IOException("Errore nella lettura del file ODS: " + e.getMessage(), e);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return Double.toString(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
