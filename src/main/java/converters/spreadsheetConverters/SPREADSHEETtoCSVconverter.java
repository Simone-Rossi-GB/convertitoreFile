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

/**
 * Classe per la conversione di file Spreadsheet (.xls/.xlsx/.ods) in formato CSV.
 * Supporta la lettura di file Excel e OpenDocument Spreadsheet, estraendo i dati e salvandoli in un file CSV.
 */
public class SPREADSHEETtoCSVconverter extends Converter {
    private static final Logger logger = LogManager.getLogger(SPREADSHEETtoCSVconverter.class); // Logger per la gestione degli errori e informazioni

    /**
     * Metodo principale per la conversione di un file Spreadsheet in CSV.
     *
     * @param spreadsheetFile Il file Spreadsheet da convertire.
     * @return Il file CSV generato.
     * @throws Exception Se il file non esiste o si verifica un errore durante la conversione.
     */
    @Override
    public File convert(File spreadsheetFile) throws Exception {
        // Controlla se il file esiste e non è corrotto
        if (!spreadsheetFile.exists() || !spreadsheetFile.isFile()) {
            logger.error("Errore durante la conversione: il file non esiste o è corrotto.");
            throw new IllegalArgumentException("Il file non esiste o è vuoto.");
        }

        logger.info("Conversione iniziata con parametri:\n | inputFile.getPath() = {}", spreadsheetFile.getPath());

        File outputFile;
        try {
            // Avvia la conversione del file
            outputFile = convertToCsv(spreadsheetFile);
            System.out.println("ritornato in convert"); // Debug: stampa per verificare il ritorno della funzione
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

    /**
     * Determina il formato del file e avvia la conversione appropriata.
     *
     * @param spreadsheetFile Il file Spreadsheet da convertire.
     * @return Il file CSV generato.
     * @throws IOException Se si verifica un errore durante la conversione.
     */
    private File convertToCsv(File spreadsheetFile) throws IOException {
        // Ottiene il nome del file e genera il nome del file CSV di output
        String fileName = spreadsheetFile.getName().toLowerCase();
        File csvFile = new File(spreadsheetFile.getParent(), fileName.replaceAll("\\.xlsx$|\\.xls$|\\.ods$", "") + ".csv");

        // Determina il formato del file e avvia la conversione
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            convertExcelToCsv(spreadsheetFile, csvFile);
        } else if (fileName.endsWith(".ods")) {
            convertOdsToCsv(spreadsheetFile, csvFile);
        } else {
            throw new IllegalArgumentException("Formato non supportato: " + fileName);
        }
        System.out.println("fileCSV tornato"); // Debug: stampa per verificare il ritorno della funzione
        return csvFile;
    }

    /**
     * Converte un file Excel (.xls, .xlsx) in CSV.
     *
     * @param excelFile Il file Excel da convertire.
     * @param csvFile Il file CSV di output.
     * @throws IOException Se si verifica un errore durante la lettura o scrittura del file.
     */
    private void convertExcelToCsv(File excelFile, File csvFile) throws IOException {
        try (InputStream inputStream = Files.newInputStream(excelFile.toPath()); // Apre il file Excel
             // Determina quale tipo di Workbook creare in base all'estensione. true = xlsx (2007+), false = xls ('97/2003)
             Workbook workbook = excelFile.getName().endsWith(".xlsx") ? new XSSFWorkbook(inputStream) : new HSSFWorkbook(inputStream);
             PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) { // Crea il file CSV di output

            // Ottiene il primo foglio del file Excel
            Sheet sheet = workbook.getSheetAt(0);
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                StringBuilder rowString = new StringBuilder(); // Stringa per costruire la riga CSV

                // Itera su tutte le celle della riga e le converte in stringa
                for (Cell cell : row) {
                    rowString.append(getCellValue(cell)).append(",");
                }

                // Scrive la riga nel file CSV, rimuovendo l'ultima virgola
                writer.println(rowString.length() > 0 ? rowString.substring(0, rowString.length() - 1) : "");
            }
        }
    }

    /**
     * Converte un file ODS (OpenDocument Spreadsheet) in CSV.
     *
     * @param odsFile Il file ODS da convertire.
     * @param csvFile Il file CSV di output.
     * @throws IOException Se si verifica un errore durante la lettura o scrittura del file.
     */
    private void convertOdsToCsv(File odsFile, File csvFile) throws IOException {
        try (SpreadsheetDocument spreadsheet = SpreadsheetDocument.loadDocument(odsFile); // Carica il file ODS
             PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) { // Crea il file CSV di output

            // Ottiene il primo foglio del file ODS
            Table table = spreadsheet.getSheetByIndex(0);
            for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
                Row row = table.getRowByIndex(rowIndex);
                StringBuilder rowString = new StringBuilder(); // Stringa per costruire la riga CSV

                // Itera su tutte le celle della riga e le converte in stringa
                for (int colIndex = 0; colIndex < row.getCellCount(); colIndex++) {
                    rowString.append(row.getCellByIndex(colIndex).getDisplayText().trim()).append(",");
                }

                // Scrive la riga nel file CSV, rimuovendo l'ultima virgola
                writer.println(rowString.length() > 0 ? rowString.substring(0, rowString.length() - 1) : "");
            }
        } catch (Exception e) {
            throw new IOException("Errore nella lettura del file ODS: " + e.getMessage(), e);
        }
    }

    /**
     * Estrae il valore di una cella Excel, gestendo diversi tipi di dati.
     *
     * @param cell La cella da cui estrarre il valore.
     * @return Il valore della cella come stringa.
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return ""; // Se la cella è vuota, restituisce una stringa vuota

        // Determina il tipo di valore della cella e lo converte in stringa
        switch (cell.getCellType()) {
            case NUMERIC:
                // Controlla se la cella contiene una data
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return Double.toString(cell.getNumericCellValue());
                }
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return ""; // Se il tipo di cella non è gestito, restituisce una stringa vuota
        }
    }
}
