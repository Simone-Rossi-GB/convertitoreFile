package converters.spreadsheetConverters;

import com.fasterxml.jackson.databind.ObjectMapper;
import converters.Converter;
import converters.exception.ConversionException;
import converters.exception.FileCreationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;

/**
 * Classe per la conversione di file Spreadsheet (.xls/.xlsx/.ods) in formato JSON.
 * Ora gestisce anche file Excel protetti da password.
 */
public class SPREADSHEETtoJSONconverter extends Converter {
    private static final Logger logger = LogManager.getLogger(SPREADSHEETtoJSONconverter.class);

    @Override
    public File convert(File spreadsheetFile) throws IOException {
        logger.info("Conversione iniziata con parametri:\n | inputFile.getPath() = {}", spreadsheetFile.getPath());
        File outputFile;

        try {
            outputFile = convertToJson(spreadsheetFile);
            if (outputFile.exists()) {
                logger.info("File convertito con successo: {}", outputFile.getName());
            } else {
                logger.error("Conversione fallita: file JSON non creato correttamente");
            }
        } catch (Exception e) {
            logger.error("Errore durante la conversione: {}", e.getMessage());
            throw new ConversionException("Errore nella conversione Spreadsheet to JSON");
        }
        return outputFile;
    }

    private File convertToJson(File spreadsheetFile) throws IOException {
        String baseName = spreadsheetFile.getName().replaceFirst("[.][^.]+$", "");
        File outputDir = new File("src/temp");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(outputDir, baseName + ".json");
        return convertSpreadsheetToJson(spreadsheetFile, outputFile.getAbsolutePath());
    }

    private File convertSpreadsheetToJson(File spreadsheetFile, String outputPath) throws IOException {
        File outputFile = new File(outputPath);

        try (InputStream inputStream = Files.newInputStream(spreadsheetFile.toPath())) {
            Workbook workbook;

            try {
                workbook = WorkbookFactory.create(inputStream);
            } catch (Exception e) {
                logger.warn("Il file potrebbe essere protetto da password. Tentativo di apertura con password...");

                String password = JOptionPane.showInputDialog(null, "Inserisci la password per il file: " + spreadsheetFile.getName(), "Password richiesta", JOptionPane.PLAIN_MESSAGE);
                if (password == null || password.isEmpty()) {
                    throw new IOException("Password non fornita, impossibile aprire il file.");
                }

                // Chiudiamo il primo stream e ne apriamo uno nuovo
                inputStream.close();
                try (InputStream protectedStream = Files.newInputStream(spreadsheetFile.toPath())) {
                    workbook = WorkbookFactory.create(protectedStream, password);
                } catch (Exception ex) {
                    logger.error("Password errata o file corrotto: {}", ex.getMessage());
                    throw new IOException("Password errata o file corrotto.");
                }
            }

            if (workbook == null) {
                throw new IOException("Impossibile aprire il file Excel.");
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
        }
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
