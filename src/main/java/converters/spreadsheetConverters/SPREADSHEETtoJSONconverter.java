package converters.spreadsheetConverters;

import com.fasterxml.jackson.databind.ObjectMapper;
import converters.Converter;
import converters.exception.ConversionException;
import converters.exception.FileCreationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.file.Files;
import java.util.*;

/**
 * Classe per la conversione di file Spreadsheet (.xls/.xlsx/.ods) in formato JSON.
 * Supporta la lettura di file Excel e OpenDocument Spreadsheet, estraendo i dati e salvandoli in un file JSON.
 */
public class SPREADSHEETtoJSONconverter extends Converter {
    private static final Logger logger = LogManager.getLogger(SPREADSHEETtoJSONconverter.class); // Logger per la gestione degli errori e informazioni

    /**
     * Metodo principale per la conversione di un file Spreadsheet in JSON.
     *
     * @param spreadsheetFile Il file Spreadsheet da convertire.
     * @return Il file JSON generato.
     * @throws IOException Se il file non esiste o si verifica un errore durante la conversione.
     */
    @Override
    public File convert(File spreadsheetFile) throws IOException {
        logger.info("Conversione iniziata con parametri:\n | outputFile.getPath() = {}", spreadsheetFile.getPath());
        File outputFile;

        try {
            // Avvia la conversione del file
            outputFile = convertToJson(spreadsheetFile);
            if (outputFile.exists()) {
                logger.info("File convertito aggiunto alla lista: {}", outputFile.getName());
            } else {
                logger.error("Conversione fallita: file JSON non creato correttamente");
            }
        } catch (Exception e) {
            logger.error("Errore durante la conversione: {}", e.getMessage());
            throw new ConversionException("Errore nella conversione Spreadsheet to JSON");
        }
        return outputFile;
    }

    /**
     * Determina il nome del file JSON e avvia la conversione.
     *
     * @param spreadsheetFile Il file Spreadsheet da convertire.
     * @return Il file JSON generato.
     * @throws IOException Se si verifica un errore durante la conversione.
     */
    private File convertToJson(File spreadsheetFile) throws IOException {
        // Usa il nome base del file e salva in src/temp/
        String baseName = spreadsheetFile.getName().replaceFirst("[.][^.]+$", "");
        File outputDir = new File("src/temp");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(outputDir, baseName + ".json");
        return convertSpreadsheetToJson(spreadsheetFile, outputFile.getAbsolutePath());
    }

    /**
     * Converte un file Spreadsheet (.xls, .xlsx) in JSON.
     *
     * @param spreadsheetFile Il file Spreadsheet da convertire.
     * @param outputPath Percorso del file JSON di output.
     * @return Il file JSON generato.
     * @throws IOException Se si verifica un errore durante la lettura o scrittura del file.
     */
    private File convertSpreadsheetToJson(File spreadsheetFile, String outputPath) throws IOException {
        File outputFile = new File(outputPath);

        try (InputStream fileStream = Files.newInputStream(spreadsheetFile.toPath());
             Workbook workbook = new HSSFWorkbook(fileStream)) { // Crea un workbook per leggere il file Excel

            // Ottiene il primo foglio del file Spreadsheet
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            List<String> headers = new ArrayList<>();
            List<Map<String, String>> dataList = new ArrayList<>();

            // Estrae le intestazioni dalla prima riga
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                for (Cell cell : headerRow) {
                    String headerValue = getCellValue(cell);
                    headers.add(headerValue.isEmpty() ? "column_" + cell.getColumnIndex() : headerValue);
                }
            }

            // Estrae i dati riga per riga
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

            // Crea la cartella di output se necessario
            createOutputDirectory(outputFile);

            // Scrive il file JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, dataList);

            logger.info("File creato: {} ({} bytes)", outputFile.getAbsolutePath(), outputFile.length());

            // Verifica che il file sia stato creato correttamente
            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile;
            } else {
                logger.error("Il file JSON non è stato creato correttamente");
                throw new FileCreationException("Il file JSON non è stato creato correttamente");
            }

        } catch (Exception e) {
            cleanupFailedConversion(outputFile);
            logger.error("Errore durante la conversione: {}", e.getMessage(), e);
            throw new ConnectException("Errore durante la conversione: " + e.getMessage());
        }
    }

    /**
     * Crea la cartella di output se non esiste.
     *
     * @param outputFile File JSON di output.
     */
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

    /**
     * Pulisce i file creati in caso di conversione fallita.
     *
     * @param outputFile File JSON da eliminare.
     */
    private void cleanupFailedConversion(File outputFile) {
        if (outputFile != null && outputFile.exists()) {
            boolean deleted = outputFile.delete();
            if (deleted) {
                System.out.println("  File parziale eliminato: " + outputFile.getName());
                logger.info("File parziale eliminato{}", outputFile.getName());
            }
        }
    }

    /**
     * Estrae il valore di una cella come stringa.
     *
     * @param cell Cella da cui estrarre il valore.
     * @return Il valore della cella come stringa.
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        try {
            return cell.getStringCellValue().trim();
        } catch (Exception ignored) {}

        try {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            } else {
                double numValue = cell.getNumericCellValue();
                return numValue == Math.floor(numValue) && !Double.isInfinite(numValue) ? String.valueOf((long) numValue) : Double.toString(numValue);
            }
        } catch (Exception ignored) {}

        try {
            return Boolean.toString(cell.getBooleanCellValue());
        } catch (Exception ignored) {}

        try {
            return cell.getCellFormula();
        } catch (Exception ignored) {}

        return "";
    }

    /**
     * Verifica se una riga è completamente vuota.
     *
     * @param row Riga da controllare.
     * @return True se la riga è vuota, false altrimenti.
     */
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
