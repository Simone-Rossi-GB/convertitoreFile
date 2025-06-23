package Converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import Converters.exception.ConvertionException;
import converter.Log;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Table;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class JSONtoODSconverter extends Converter {

    private static final Logger logger = LogManager.getLogger(JSONtoODSconverter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public File convert(File srcFile) throws Exception, ConvertionException {
        if (controlloFileNonVuoto(srcFile)) {
            return convertInternal(srcFile, null, false);
        }
        logger.error("File vuoto o corrotto: {}", srcFile.getName());
        Log.addMessage("[JSON→ODS] ERRORE: file vuoto o corrotto - " + srcFile.getName());
        throw new ConvertionException("File vuoto o corrotto");
    }

    /**
     * Controlla se il file è vuoto.
     * @param srcFile Il file da verificare.
     * @return true se il file NON è vuoto, false se è vuoto o nullo.
     */
    private boolean controlloFileNonVuoto(File srcFile) {
        return srcFile != null && srcFile.length() > 0;
    }

    private File convertInternal(File jsonFile, String password, boolean opzioni) throws Exception {
        //logger.info("Inizio conversione con parametri: \n | srcFile.getPath() = {}", srcFile.getPath());
        Log.addMessage("[JSON→ODS] Inizio conversione file: " + jsonFile.getName());

        List<LinkedHashMap<String, Object>> data;
        try {
            data = objectMapper.readValue(
                    jsonFile,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, LinkedHashMap.class)
            );
        } catch (Exception e) {
            logger.error("Parsing JSON fallito: {}", e.getMessage());
            Log.addMessage("[JSON→ODS] ERRORE: parsing del file JSON fallito.");
            throw e;
        }

        if (data.isEmpty()) {
            throw new IllegalArgumentException("Il file JSON è vuoto o malformato.");
        }

        // Creazione nuovo documento .ods
        File outFile;
        try (SpreadsheetDocument document = SpreadsheetDocument.newSpreadsheetDocument()) {
            Table sheet = document.getSheetByIndex(0);
            sheet.setTableName("Dati");

            // Intestazioni
            Set<String> headers = data.get(0).keySet();
            int colIndex = 0;
            for (String header : headers) {
                sheet.getCellByPosition(colIndex++, 0).setStringValue(header);
            }

            // Dati
            int rowIndex = 1;
            for (Map<String, Object> row : data) {
                colIndex = 0;
                for (String header : headers) {
                    Object value = row.get(header);
                    sheet.getCellByPosition(colIndex++, rowIndex).setStringValue(value != null ? value.toString() : "");
                }
                rowIndex++;
            }

            // Scrittura su file temporaneo .ods
            outFile = File.createTempFile("converted-", ".ods");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                document.save(fos);
            }
        } catch (Exception e) {
            logger.error("Generazione del file ODS fallita: {}", e.getMessage());
            Log.addMessage("[JSON→ODS] ERRORE: generazione del file ODS fallita.");
            throw e;
        }

        logger.info("Conversione completata: {}", outFile.getName());
        Log.addMessage("[JSON→ODS] Conversione completata con successo: " + outFile.getName());

        return outFile;
    }
}

