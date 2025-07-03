package webService.server.converters.spreadsheetConverters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import webService.server.config.configHandlers.Config;
import webService.server.converters.ConverterDocumentsWithPasword;
import webService.server.converters.exception.ConversionException;
import webService.server.converters.exception.FileCreationException;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

/**
 * Classe per la conversione di file Spreadsheet (.xls/.xlsx/.ods) in formato JSON.
 * Ora gestisce anche file Excel protetti da password.
 */
public class SPREADSHEETtoJSONconverter extends ConverterDocumentsWithPasword {
    private static final Logger logger = LogManager.getLogger(SPREADSHEETtoJSONconverter.class);

    @Override
    public File convertProtectedFile(File spreadsheetFile, String password, Config configuration) throws IllegalArgumentException, ConversionException {
        logger.info("Conversione iniziata con parametri:\n | inputFile.getPath() = {}", spreadsheetFile.getPath());

        // Controlla se il file esiste e non è corrotto
        if (!spreadsheetFile.exists() || !spreadsheetFile.isFile()) {
            logger.error("Errore durante la conversione: il file non esiste o è corrotto");
            throw new IllegalArgumentException("File vuoto o corrotto");
        }

        File outputFile;
        try {
            // Controlla se il file è protetto da password
            boolean isProtected = checkForPassword(password);
            logger.info(isProtected ? "Il file è protetto da password, tentativo di apertura con password..." : "Il file NON è protetto da password, apertura normale...");

            // Determina il formato e avvia la conversione
            outputFile = convertToJson(spreadsheetFile, isProtected ? password : null);

            if (outputFile.exists()) {
                logger.info("File convertito con successo: {}", outputFile.getName());
            } else {
                logger.error("Conversione fallita: file JSON non creato correttamente");
                throw new IOException("File JSON non creato correttamente");
            }
        } catch (Exception e) {
            logger.error("Errore durante la conversione: {}", e.getMessage());
            throw new ConversionException("Errore nella conversione Spreadsheet to JSON");
        }
        return outputFile;
    }

    private File convertToJson(File spreadsheetFile, String password) throws IOException {
        String baseName = spreadsheetFile.getName().replaceFirst("[.][^.]+$", "");
        String outputPath = new File("src/temp", baseName + ".json").getAbsolutePath();
        return convertSpreadsheetToJson(spreadsheetFile, outputPath, password);
    }

    private File convertSpreadsheetToJson(File spreadsheetFile, String outputPath, String password) throws IOException {
        File outputFile = new File(outputPath);
        Workbook workbook = null;
        String fileName = spreadsheetFile.getName().toLowerCase();

        try {
            if (password != null && !password.isEmpty()) {
                try (FileInputStream fileInputStream = new FileInputStream(spreadsheetFile.getAbsoluteFile())) {
                    if (fileName.endsWith(".xls")) {
                        Biff8EncryptionKey.setCurrentUserPassword(password);
                        workbook = new HSSFWorkbook(fileInputStream);
                    } else if (fileName.endsWith(".xlsx")) {
                        workbook = WorkbookFactory.create(fileInputStream, password);
                    }
                }
            } else {
                try (InputStream inputStream = Files.newInputStream(spreadsheetFile.toPath())) {
                    workbook = WorkbookFactory.create(inputStream);
                }
            }

            if (workbook == null) {
                throw new IOException("Impossibile aprire il file Excel: crittografia non supportata");
            }

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            List<String> headers = new ArrayList<>();
            List<Map<String, String>> dataList = new ArrayList<>();

            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                for (Cell cell : headerRow) {
                    String headerValue = getCellValue(cell);
                    headers.add(headerValue.isEmpty() ? "column_" + cell.getColumnIndex() : headerValue);
                }
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isRowEmpty(row)) {
                    continue;
                }

                Map<String, String> rowData = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(i), getCellValue(cell));
                }

                dataList.add(rowData);
            }

            createOutputDirectory(outputFile);

            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, dataList);

            logger.info("File creato: {} ({} bytes)", outputFile.getAbsolutePath(), outputFile.length());

            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile;
            } else {
                logger.error("Il file JSON non è stato creato correttamente");
                throw new FileCreationException("Il file JSON non è stato creato correttamente");
            }

        } catch (Exception e) {
            cleanupFailedConversion(outputFile);
            logger.error("Errore durante la conversione: {}", e.getMessage(), e);
            throw new IOException("Errore durante la conversione: " + e.getMessage());
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
    }

    private boolean checkForPassword(String password) {
        return !(password == null || password.isEmpty());
    }

    private void createOutputDirectory(File outputFile) {
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                throw new FileCreationException("Impossibile creare la cartella: " + parentDir.getAbsolutePath());
            }
            System.out.println("  Cartella creata: " + parentDir.getAbsolutePath());
        }
    }

    private void cleanupFailedConversion(File outputFile) {
        if (outputFile != null && outputFile.exists()) {
            boolean deleted = outputFile.delete();
            if (deleted) {
                System.out.println("  File parziale eliminato: " + outputFile.getName());
                logger.info("File parziale eliminato{}", outputFile.getName());
            }
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

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

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (Cell cell : row) {
            if (cell != null && !getCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
