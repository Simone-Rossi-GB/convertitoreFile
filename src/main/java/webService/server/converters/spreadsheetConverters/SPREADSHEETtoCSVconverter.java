package webService.server.converters.spreadsheetConverters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;
import webService.server.config.configHandlers.Config;
import webService.server.converters.ConverterDocumentsWithPasword;
import webService.server.converters.exception.ConversionException;
import webService.server.converters.exception.EmptyFileException;

import java.io.*;
import java.nio.file.Files;

/**
 * Classe per la conversione di file Spreadsheet (.xls/.xlsx/.ods) in formato CSV.
 * Supporta la lettura di file Excel e OpenDocument Spreadsheet, estraendo i dati e salvandoli in un file CSV.
 * Ora gestisce anche file Excel protetti da password.
 */
public class SPREADSHEETtoCSVconverter extends ConverterDocumentsWithPasword {
    private static final Logger logger = LogManager.getLogger(SPREADSHEETtoCSVconverter.class);

    @Override
    public File convertProtectedFile(File spreadsheetFile, String password, Config configuration) throws IllegalArgumentException, ConversionException {
        System.out.println("=================||INIZIO CONVERTITORE||=======================");
        System.out.println("password: " + password);

        // Controlla se il file esiste e non è corrotto
        if (!spreadsheetFile.exists() || !spreadsheetFile.isFile()) {
            logger.error("Errore durante la conversione: il file non esiste o è corrotto");
            throw new IllegalArgumentException("File vuoto o corrotto");
        }

        logger.info("Conversione iniziata con parametri:\n | inputFile.getPath() = {}", spreadsheetFile.getPath());

        File outputFile;
        try {
            // Controlla se il file è protetto da password
            boolean isProtected = checkForPassword(password);
            logger.info(isProtected ? "Il file è protetto da password, tentativo di apertura con password..." : "Il file NON è protetto da password, apertura normale...");

            // Determina il formato e avvia la conversione
            System.out.println("==========================||prima getFileFormat||=================================");
            outputFile = getFileFormat(spreadsheetFile, isProtected ? password : null);
            System.out.println("==========================||dopo getFileFormat||=================================");


            // Controlla se il file CSV è stato creato correttamente
            if (outputFile.exists()) {
                System.out.println("===========================|| outputFile esiste ||==================================");

                logger.info("File convertito con successo: {}", outputFile.getName());
            } else {
                logger.error("Conversione fallita: file CSV non creato correttamente");
                throw new IOException("File CSV non creato correttamente");
            }
        } catch (Exception e) {
            logger.error("Errore durante la conversione: {}", e.getMessage());
            throw new ConversionException("Errore nella conversione Spreadsheet to CSV");
        }
        System.out.println("====================||return outputFile||=======================");
        return outputFile;
    }

    private File getFileFormat(File spreadsheetFile, String password) throws Exception {
        System.out.println("===========================|||| dentro getFileFormat |||==================================");

        String fileName = spreadsheetFile.getName().toLowerCase();
        File csvFile = new File(spreadsheetFile.getParent(), fileName.replaceAll("\\.xlsx$|\\.xls$|\\.ods$", "") + ".csv");

        // Determina il formato del file e chiama il metodo appropriato
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            convertExcelToCsv(spreadsheetFile, csvFile, password);
        } else if (fileName.endsWith(".ods")) {
            convertOdsToCsv(spreadsheetFile, csvFile);
        } else {
            throw new IllegalArgumentException("Formato non supportato: " + fileName);
        }
        System.out.println("========================||||return csvFile|||============================");
        return csvFile;
    }

    private boolean checkForPassword(String password) {
        return !(password == null || password.isEmpty());
    }

    private void convertExcelToCsv(File excelFile, File csvFile, String password) throws IOException, EmptyFileException {
        System.out.println("==========================|||dentro convertExcelToCsv||===================================");

        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            System.out.println("===========================||| dentro try 1 ||==================================");
            Workbook workbook = null;
            System.out.println("===========================||| workbook creato ||==================================");

            String fileName = excelFile.getName().toLowerCase();
            System.out.println("===========================||| fileName creato ||==================================");

            try {
                System.out.println("===========================||| dentro try 2 ||==================================");

                if (password != null && !password.isEmpty()) {
                    System.out.println("===========================||| esiste password ||==================================");

                    try (FileInputStream fileInputStream = new FileInputStream(excelFile.getAbsoluteFile())) {
                        System.out.println("===========================||| dentro try 3 ||==================================");

                        if (fileName.endsWith(".xls")) {
                            System.out.println("===========================||| if(xls) ||==================================");

                            // File Excel 97-2003 (.xls)
                            Biff8EncryptionKey.setCurrentUserPassword(password);
                            System.out.println("===========================||| gestione crittografia xls ||==================================");

                            try {
                                workbook = new HSSFWorkbook(fileInputStream);
                                System.out.println("===========================||| workbook xls protetto ||==================================");
                            } catch (Exception ex) {
                                System.err.println("Errore: Crittografia non supportata per XLS.");
                                logger.error("Errore nell'apertura file protetto: {}", ex.getMessage());
                                throw new IOException("Crittografia non supportata per XLS.");
                            }

                        } else if (fileName.endsWith(".xlsx")) {
                            System.out.println("===========================||| if(xlsx) ||==================================");

                            try {
                                workbook = WorkbookFactory.create(fileInputStream, password);
                                System.out.println("===========================||| workbook xlsx protetto ||==================================");
                            } catch (Exception ex) {
                                System.err.println("Errore: Crittografia non supportata per XLSX.");
                                logger.error("Errore nell'apertura file protetto: {}", ex.getMessage());
                                throw new IOException("Crittografia non supportata per XLSX.");
                            }
                        }
                    }
                } else {
                    System.out.println("===========================||| file non protetto ||==================================");

                    // Se il file NON è protetto, lo apre normalmente
                    try (InputStream inputStream = Files.newInputStream(excelFile.toPath())) {
                        System.out.println("===========================||| dentro try 4 ||==================================");

                        try {
                            workbook = WorkbookFactory.create(inputStream);
                            System.out.println("===========================||| apre workbook generico excel ||==================================");
                        } catch (Exception ex) {
                            System.err.println("Errore: Il file potrebbe essere corrotto o non supportato.");
                            logger.error("Errore nell'apertura file non protetto: {}", ex.getMessage());
                            throw new IOException("Errore nell'apertura file non protetto.");
                        }
                    }
                }

                // Verifica che il workbook sia stato caricato correttamente
                if (workbook == null) {
                    logger.error("Impossibile aprire il file Excel: crittografia non supportata");
                    throw new IOException("Impossibile aprire il file Excel: crittografia non supportata");
                }
                System.out.println("===========================||| workbook esiste ||==================================");

                System.out.println("===========================||| inizia scrittura csv ||==================================");
                // Itera sulle righe e celle, convertendo i dati in CSV
                Sheet sheet = workbook.getSheetAt(0);
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    StringBuilder rowString = new StringBuilder();
                    for (Cell cell : row) {
                        rowString.append(getCellValue(cell)).append(",");
                    }
                    writer.println(rowString.length() > 0 ? rowString.substring(0, rowString.length() - 1) : "");
                }
                System.out.println("===========================|| termina scrittura csv ||==================================");
            } finally {
                if (workbook != null) {
                    System.out.println("===========================|| chiude workbook ||==================================");
                    workbook.close(); // Chiude il workbook per evitare memory leaks
                }
            }
        }
    }




    private void convertOdsToCsv(File odsFile, File csvFile) throws Exception {
        try (SpreadsheetDocument spreadsheet = SpreadsheetDocument.loadDocument(odsFile);
             PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {

            Table table = spreadsheet.getSheetByIndex(0);
            for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
                Row row = table.getRowByIndex(rowIndex);
                StringBuilder rowString = new StringBuilder();
                for (int colIndex = 0; colIndex < row.getCellCount(); colIndex++) {
                    rowString.append(row.getCellByIndex(colIndex).getDisplayText().trim()).append(",");
                }
                writer.println(rowString.length() > 0 ? rowString.substring(0, rowString.length() - 1) : "");
            }
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case NUMERIC:
                return DateUtil.isCellDateFormatted(cell) ? cell.getDateCellValue().toString() : Double.toString(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
