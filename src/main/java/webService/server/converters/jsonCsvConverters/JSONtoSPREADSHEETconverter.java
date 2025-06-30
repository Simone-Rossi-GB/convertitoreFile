package webService.server.converters.jsonCsvConverters;

import webService.server.configuration.configHandlers.conversionContext.ConversionContextReader;
import webService.server.converters.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

/**
 * Classe per la conversione di un file JSON in un file Spreadsheet (.xls/.xlsx/.ods).
 * Utilizza Apache POI per la gestione dei fogli di calcolo e applica uno stile personalizzato alle celle.
 * Inoltre, implementa una logica per eliminare le colonne vuote, migliorando la leggibilità del file Excel generato.
 */
public class JSONtoSPREADSHEETconverter extends Converter {
    public static final Logger logger = LogManager.getLogger(JSONtoSPREADSHEETconverter.class); // Logger per la gestione degli errori e informazioni
    private boolean[] flagEndColumns; // Array di flag per controllare le colonne vuote

    /**
     * Metodo principale per la conversione di un file JSON in un file Spreadsheet.
     *
     * @param srcFile Il file JSON da convertire.
     * @return Il file Spreadsheet generato.
     * @throws IOException Se il file non esiste o si verifica un errore durante la conversione.
     */
    @Override
    public File convert(File srcFile) throws IOException {
        // Controlla se il file esiste e non è corrotto
        if (!srcFile.exists() || !srcFile.isFile()) {
            logger.error("File vuoto o corrotto");
            throw new IOException("Il file vuoto o corrotto");
        }

        // Legge il contenuto del file JSON
        String jsonContent = readJsonFile(srcFile);
        JSONArray jsonArray = new JSONArray(jsonContent);

        // Converte JSONArray in una lista per mantenere l'ordine originale
        List<JSONObject> jsonObjects = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            jsonObjects.add(jsonArray.getJSONObject(i));
        }

        // Crea un nuovo Workbook in formato .xls
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Dati");

        // Ottiene le colonne dal JSON
        Set<String> colonne = getKeyColumn(jsonObjects);

        // Inizializza flagEndColumns con tutti i valori a true
        flagEndColumns = new boolean[colonne.size()];
        Arrays.fill(flagEndColumns, true);

        // Scrive i dati nel foglio
        writeJsonToSheet(jsonObjects, sheet, workbook, colonne);

        // Ridimensiona automaticamente le colonne
        autoSizeColumns(sheet, colonne.size());

        // Genera il nome del file di output
        String outputFileName = srcFile.getName().replaceAll("\\.json$", "") + "." + ConversionContextReader.getDestinationFormat();
        File outputFile = new File(srcFile.getParent(), outputFileName);

        // Scrive il file Excel
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }
        workbook.close();

        logger.info("Conversione completata con successo");
        return outputFile;
    }

    /**
     * Legge il contenuto di un file JSON e lo restituisce come stringa.
     *
     * @param jsonFile Il file JSON da leggere.
     * @return Il contenuto del file JSON come stringa.
     * @throws IOException Se il file è vuoto o corrotto.
     */
    private String readJsonFile(File jsonFile) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            logger.error("File vuoto o corrotto");
            throw new IOException("File vuoto o corrotto");
        }
    }

    /**
     * Determina le colonne del file Excel in base alle chiavi presenti nel JSON.
     *
     * @param jsonObjects Lista di oggetti JSON.
     * @return Set di colonne da includere nel file Excel.
     */
    private Set<String> getKeyColumn(List<JSONObject> jsonObjects) {
        Set<String> colonne = new LinkedHashSet<>(); // Mantiene l'ordine originale
        for (JSONObject jsonObject : jsonObjects) {
            colonne.addAll(jsonObject.keySet());
        }
        return colonne;
    }

    /**
     * Scrive i dati JSON nel foglio Excel, eliminando le colonne vuote.
     *
     * @param jsonObjects Lista di oggetti JSON.
     * @param sheet Foglio Excel in cui scrivere i dati.
     * @param workbook Workbook contenente il foglio.
     * @param colonne Set di colonne da includere.
     * @throws IOException Se il file JSON è vuoto o corrotto.
     */
    private void writeJsonToSheet(List<JSONObject> jsonObjects, Sheet sheet, Workbook workbook, Set<String> colonne) throws IOException {
        if (jsonObjects.isEmpty()) {
            logger.error("Il file JSON è vuoto");
            throw new IOException("Il file è vuoto o corrotto");
        }

        // Crea la riga di intestazione con i nomi delle colonne
        Row headerRow = sheet.createRow(0);
        int colNum = 0;

        // Inverte l'ordine delle colonne per mantenere la struttura originale
        List<String> colonneList = new ArrayList<>();
        List<String> listAppoggio = new ArrayList<>(colonne);
        for (int index = listAppoggio.size() - 1; index >= 0; index--) {
            colonneList.add(listAppoggio.get(index));
        }

        // Crea gli stili per le celle
        CellStyle headerStyle = createStyle(workbook, true);
        CellStyle rowStyle = createStyle(workbook, false);

        // Scrive le intestazioni delle colonne
        for (String key : colonneList) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(key);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;

        // Scrive i dati nel foglio Excel
        for (JSONObject jsonObject : jsonObjects) {
            Row row = sheet.createRow(rowNum);
            colNum = 0;

            for (String key : colonneList) {
                if (flagEndColumns[colNum]) {
                    String value = jsonObject.optString(key, "");

                    if (value.isEmpty()) {
                        boolean hasValue = false;
                        for (int j = rowNum; j < jsonObjects.size(); j++) {
                            JSONObject nextObject = jsonObjects.get(j);
                            if (!nextObject.optString(key, "").isEmpty()) {
                                hasValue = true;
                                break;
                            }
                        }

                        if (!hasValue) {
                            flagEndColumns[colNum] = false;
                        }
                    }

                    if (flagEndColumns[colNum]) {
                        Cell cell = row.createCell(colNum);
                        cell.setCellValue(value);
                        cell.setCellStyle(rowStyle);
                    }
                }
                colNum++;
            }
            rowNum++;
        }
    }

    /**
     * Ridimensiona automaticamente le colonne del foglio Excel.
     *
     * @param sheet Foglio Excel.
     * @param columnCount Numero totale di colonne.
     */
    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int colNum = 0; colNum < columnCount; colNum++) {
            sheet.autoSizeColumn(colNum);
        }
    }

    /**
     * Crea uno stile per le celle, con intestazioni colorate.
     *
     * @param workbook Workbook in cui applicare lo stile.
     * @param isHeader True se lo stile è per l'intestazione, false per le celle normali.
     * @return Lo stile creato.
     */
    private CellStyle createStyle(Workbook workbook, boolean isHeader) {
        CellStyle style = workbook.createCellStyle();

        // Allineamento orizzontale e verticale
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Imposta il colore di sfondo
        style.setFillForegroundColor(isHeader ? IndexedColors.GREY_25_PERCENT.getIndex() : IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Aggiunta dei bordi neri su tutti i lati
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());

        return style;
    }
}
