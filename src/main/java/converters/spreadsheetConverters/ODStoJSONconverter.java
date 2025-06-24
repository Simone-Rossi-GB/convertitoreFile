package converters.spreadsheetConverters;

import com.fasterxml.jackson.databind.ObjectMapper;
import converters.Converter;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;
import converters.exception.UnsupportedConversionException;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Classe responsabile della conversione di file ODS (OpenDocument Spreadsheet)
 * in formato JSON.
 */
public class ODStoJSONconverter extends Converter {

    private static final Logger logger = LogManager.getLogger(ODStoJSONconverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Converte un file ODS in JSON.
     *
     * @param srcFile File ODS da convertire
     * @return Lista contenente il file JSON generato
     * @throws Exception           In caso di errori generici
     * @throws UnsupportedConversionException Se il file è nullo, vuoto o non valido
     */
    @Override
    public File convert(File srcFile) throws Exception, UnsupportedConversionException {
        if (controlloFileNonVuoto(srcFile)) {
            return convertInternal(srcFile);
        }
        logger.error("File nullo o vuoto con password specificata: {}", srcFile);
        throw new UnsupportedConversionException("L'oggetto srcFile non esiste o è vuoto.");
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
    private File convertInternal(File odsFile) throws Exception {
        if (odsFile == null) {
            logger.error("File ODS nullo");
            throw new IllegalArgumentException("L'oggetto odsFile non esiste.");
        }

        logger.info("Inizio conversione con parametri: \n | odsFile.getPath() = {}", odsFile.getPath());
        List<Map<String, Object>> jsonData = new ArrayList<>();
        SpreadsheetDocument spreadsheet;

        try {
            spreadsheet = SpreadsheetDocument.loadDocument(odsFile);
        } catch (Exception e) {
            logger.error("Errore apertura documento ODS: {}", e.getMessage());
            throw new UnsupportedConversionException("Errore nel caricamento del file ODS.");
        }

        Table table = spreadsheet.getSheetByIndex(0);
        if (table == null) {
            logger.error("Nessuna tabella trovata nel documento");
            throw new UnsupportedConversionException("L'oggetto table non esiste nel file ODS.");
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
        } catch (Exception e) {
            logger.error("Scrittura file JSON fallita: {}", e.getMessage());
            throw new UnsupportedConversionException("Errore durante la scrittura del file JSON.");
        }

        logger.info("Conversione completata: {}", outFile.getAbsolutePath());

        return outFile;
    }
}
