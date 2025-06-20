package Converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Row;
import org.odftoolkit.simple.table.Table;
import converter.ConvertionException;
import converter.Log;

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

    private static final Logger logger = LogManager.getLogger(ODStoJSONconverter.class);
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
            return convertInternal(srcFile, null, false);
        }
        logger.error("File nullo o vuoto: {}", srcFile);
        Log.addMessage("[ODS→JSON] ERRORE: file nullo o vuoto.");
        throw new ConvertionException("L'oggetto srcFile non esiste o è vuoto.");
    }

    /**
     * Converte un file ODS protetto da password (non usata in questa implementazione) in JSON.
     *
     * @param srcFile  File ODS da convertire
     * @param password Password del file (non usata)
     * @return Lista contenente il file JSON generato
     * @throws Exception           In caso di errori generici
     * @throws ConvertionException Se il file è nullo, vuoto o non valido
     */
    @Override
    public ArrayList<File> convert(File srcFile, String password) throws Exception, ConvertionException {
        if (controlloFileNonVuoto(srcFile)) {
            return convertInternal(srcFile, password, false);
        }
        logger.error("File nullo o vuoto con password specificata: {}", srcFile);
        Log.addMessage("[ODS→JSON] ERRORE: file nullo o vuoto.");
        throw new ConvertionException("L'oggetto srcFile non esiste o è vuoto.");
    }

    /**
     * Converte un file ODS in JSON applicando opzioni personalizzate.
     *
     * @param srcFile File ODS da convertire
     * @param opzioni Opzioni di elaborazione (non usate in questa versione)
     * @return Lista contenente il file JSON generato
     * @throws Exception           In caso di errori generici
     * @throws ConvertionException Se il file è nullo, vuoto o non valido
     */
    @Override
    public ArrayList<File> convert(File srcFile, boolean opzioni) throws Exception {
        if (controlloFileNonVuoto(srcFile)) {
            return convertInternal(srcFile, null, opzioni);
        }
        logger.error("File nullo o vuoto con opzioni: {}", srcFile);
        Log.addMessage("[ODS→JSON] ERRORE: file nullo o vuoto.");
        throw new ConvertionException("L'oggetto srcFile non esiste o è vuoto.");
    }

    /**
     * Converte un file ODS in JSON utilizzando password e opzioni (non utilizzati).
     *
     * @param srcFile  File ODS da convertire
     * @param password Password del file (non usata)
     * @param opzioni  Opzioni di elaborazione (non usate)
     * @return Lista contenente il file JSON generato
     * @throws Exception           In caso di errori generici
     * @throws ConvertionException Se il file è nullo, vuoto o non valido
     */
    @Override
    public ArrayList<File> convert(File srcFile, String password, boolean opzioni) throws Exception {
        if (controlloFileNonVuoto(srcFile)) {
            return convertInternal(srcFile, password, opzioni);
        }
        logger.error("File nullo o vuoto con password/opzioni: {}", srcFile);
        Log.addMessage("[ODS→JSON] ERRORE: file nullo o vuoto.");
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
     * @param password Password (non usata)
     * @param opzioni  Opzioni (non usate)
     * @return Lista contenente un singolo file JSON generato
     * @throws Exception In caso di errori nella lettura o scrittura file
     */
    private ArrayList<File> convertInternal(File odsFile, String password, boolean opzioni) throws Exception {
        if (odsFile == null) {
            logger.error("File ODS nullo");
            Log.addMessage("[ODS→JSON] ERRORE: oggetto file nullo.");
            throw new IllegalArgumentException("L'oggetto odsFile non esiste.");
        }

        logger.info("Inizio conversione con parametri: \n | odsFile.getPath() = {}", odsFile.getPath());
        Log.addMessage("[ODS→JSON] Inizio conversione file: " + odsFile.getName());

        List<Map<String, Object>> jsonData = new ArrayList<>();
        SpreadsheetDocument spreadsheet;

        try {
            spreadsheet = SpreadsheetDocument.loadDocument(odsFile);
        } catch (Exception e) {
            logger.error("Errore apertura documento ODS: {}", e.getMessage());
            Log.addMessage("[ODS→JSON] ERRORE: apertura documento fallita.");
            throw new ConvertionException("Errore nel caricamento del file ODS.");
        }

        Table table = spreadsheet.getSheetByIndex(0);
        if (table == null) {
            logger.error("Nessuna tabella trovata nel documento");
            Log.addMessage("[ODS→JSON] ERRORE: nessuna tabella presente nel file.");
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
        } catch (Exception e) {
            logger.error("Scrittura file JSON fallita: {}", e.getMessage());
            Log.addMessage("[ODS→JSON] ERRORE: scrittura del file JSON fallita.");
            throw new ConvertionException("Errore durante la scrittura del file JSON.");
        }

        logger.info("Conversione completata: {}", outFile.getAbsolutePath());
        Log.addMessage("[ODS→JSON] Creazione file .json completata: " + outFile.getName());

        ArrayList<File> output = new ArrayList<>();
        output.add(outFile);
        return output;
    }
}
