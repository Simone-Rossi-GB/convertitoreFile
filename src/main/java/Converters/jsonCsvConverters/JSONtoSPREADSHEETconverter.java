package converters.jsonCsvConverters;

import configuration.configHandlers.conversionContext.ConversionContextReader;
import converters.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public abstract class JSONtoSPREADSHEETconverter extends Converter {
    public static final Logger logger = LogManager.getLogger(JSONtoSPREADSHEETconverter.class);
    private final TreeMap<String, Short> coloriCelle = new TreeMap<>();

    private boolean reverseOrder = true; // Flag per determinare la direzione
    private String mapFollower; // Contatore per l'indice dei colori
    private boolean[] flagElementi; // Array di flag per controllare gli elementi

    private void colorBuilding() {
        // Popola la mappa coloriCelle con i valori forniti
        coloriCelle.put("06065D", IndexedColors.BLUE.getIndex());
        coloriCelle.put("0504AA", IndexedColors.INDIGO.getIndex());
        coloriCelle.put("2337C6", IndexedColors.BLUE_GREY.getIndex());
        coloriCelle.put("395A7F", IndexedColors.GREY_40_PERCENT.getIndex());
        coloriCelle.put("4169E1", IndexedColors.ROYAL_BLUE.getIndex());
        coloriCelle.put("0A9396", IndexedColors.TEAL.getIndex());
        coloriCelle.put("90E0EF", IndexedColors.LIGHT_BLUE.getIndex());
        coloriCelle.put("C4C4DF", IndexedColors.LAVENDER.getIndex());
        coloriCelle.put("FFFFFF", IndexedColors.WHITE.getIndex());
    }


    @Override
    public File convert(File srcFile) throws IOException {
        colorBuilding();
        if (!srcFile.exists() || !srcFile.isFile()) {
            logger.error("File non valido");
            throw new IOException("Il file JSON non esiste o non è valido.");
        }

        // Leggi il file JSON
        String jsonContent = readJsonFile(srcFile);
        JSONArray jsonArray = new JSONArray(jsonContent);

        // Inizializza i flagElementi in base al numero di colonne della prima posizione
        JSONObject firstObject = jsonArray.getJSONObject(0);
        flagElementi = new boolean[firstObject.keySet().size()];
        // Tutti i flag inizializzati a true
        Arrays.fill(flagElementi, true);

        // Crea il file Excel
        Workbook workbook = new HSSFWorkbook(); // Formato .xls
        Sheet sheet = workbook.createSheet("Dati");

        // Determina il numero massimo di colonne
        Set<String> colonne = getMaxColumns(jsonArray);

        // Scrivi i dati nel file Excel
        writeJsonToSheet(jsonArray, sheet, workbook, colonne);

        // Adatta automaticamente la larghezza delle colonne
        autoSizeColumns(sheet, colonne.size());

        // Genera il nome del file di output con la stessa base del file JSON
        String outputFileName = srcFile.getName().replaceAll("\\.json$", "") + "." + ConversionContextReader.getDestinationFormat();
        File outputFile = new File(srcFile.getParent(), outputFileName);

        // Salva il file Excel
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }
        workbook.close();

        logger.info("Conversione completata con successo");
        return outputFile;
    }

    private String readJsonFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }

    private Set<String> getMaxColumns(JSONArray jsonArray) {
        Set<String> colonne = new HashSet<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            colonne.addAll(jsonObject.keySet());
        }
        return colonne;
    }

    private void writeJsonToSheet(JSONArray jsonArray, Sheet sheet, Workbook workbook, Set<String> colonne) throws IOException {
        if (jsonArray.isEmpty()) {
            logger.error("Il file JSON è vuoto");
            throw new IOException("Il file è vuoto o corrotto");
        }

        // Creazione dell'intestazione
        Row headerRow = sheet.createRow(0);
        int colNum = 0;
        for (String key : colonne) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(key);
            cell.setCellStyle(createStyle(workbook, true)); // Stile per l'intestazione
        }

        // Scrittura dei dati
        int rowNum = 1;
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            Row row = sheet.createRow(rowNum);
            colNum = 0;

            // Incrementa arrayFollower solo all'inizio di ogni nuova riga
            updateMapFollower();

            CellStyle rowStyle = createStyle(workbook, false); // Stile uniforme per tutta la riga

            for (String key : colonne) {
                // Controlla il flag prima di scrivere nella cella
                if (flagElementi[colNum]) {
                    String value = jsonObject.optString(key, null);
                    if (value != null && !value.isEmpty()) {
                        Cell cell = row.createCell(colNum);
                        cell.setCellValue(value);
                        cell.setCellStyle(rowStyle); // Applica lo stesso stile a tutte le celle della riga
                    } else {
                        // Se il valore è vuoto, aggiorna il flag a false
                        flagElementi[colNum] = false;
                    }
                }
                colNum++;
            }
            rowNum++;
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int colNum = 0; colNum < columnCount; colNum++) {
            sheet.autoSizeColumn(colNum);
        }
    }

    private void updateMapFollower() {
        // Ottieni l'insieme ordinato delle chiavi
        Set<String> keys = coloriCelle.keySet();
        List<String> keyList = new ArrayList<>(keys); // Converti in una lista per accedere agli indici

        // Trova l'indice corrente di mapFollower
        int currentIndex = keyList.indexOf(mapFollower);

        // Determina il prossimo indice in base alla direzione
        if (reverseOrder) {
            currentIndex++; // Vai avanti
            if (currentIndex >= keyList.size()) {
                // Se superi l'ultima chiave, inverti la direzione
                reverseOrder = false;
                currentIndex = keyList.size() - 2; // Torna indietro di uno
            }
        } else {
            currentIndex--; // Vai indietro
            if (currentIndex < 0) {
                // Se superi la prima chiave, inverti la direzione
                reverseOrder = true;
                currentIndex = 1; // Vai avanti di uno
            }
        }

        // Aggiorna mapFollower con la nuova chiave
        mapFollower = keyList.get(currentIndex);
    }

    private CellStyle createStyle(Workbook workbook, boolean isHeader) {
        CellStyle style = workbook.createCellStyle();

        // Allineamento orizzontale e verticale
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        if (isHeader) {
            // Stile speciale per l'intestazione
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        } else {
            // Applica il colore corrispondente
            short colorIndex = coloriCelle.get(mapFollower);
            style.setFillForegroundColor(colorIndex);
        }

        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
