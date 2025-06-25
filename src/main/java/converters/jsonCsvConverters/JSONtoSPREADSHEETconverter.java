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

public class JSONtoSPREADSHEETconverter extends Converter {
    public static final Logger logger = LogManager.getLogger(JSONtoSPREADSHEETconverter.class);
    private boolean[] flagEndColumns; // Array di flag per controllare le colonne vuote

    @Override
    public File convert(File srcFile) throws IOException {

        if (!srcFile.exists() || !srcFile.isFile()) {
            logger.error("File vuoto o corrotto");
            throw new IOException("Il file vuoto o corrotto");
        }

        String jsonContent = readJsonFile(srcFile);
        JSONArray jsonArray = new JSONArray(jsonContent);

        // Converti JSONArray in una lista per mantenere l'ordine originale
        List<JSONObject> jsonObjects = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            jsonObjects.add(jsonArray.getJSONObject(i)); // Mantiene l'ordine originale
        }

        Workbook workbook = new HSSFWorkbook(); // Formato .xls
        Sheet sheet = workbook.createSheet("Dati");

        Set<String> colonne = getKeyColumn(jsonObjects);

        // Inizializza flagEndColumns con tutti i valori a true
        flagEndColumns = new boolean[colonne.size()];
        Arrays.fill(flagEndColumns, true);

        writeJsonToSheet(jsonObjects, sheet, workbook, colonne);

        autoSizeColumns(sheet, colonne.size());

        String outputFileName = srcFile.getName().replaceAll("\\.json$", "") + "." + ConversionContextReader.getDestinationFormat();
        File outputFile = new File(srcFile.getParent(), outputFileName);

        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }
        workbook.close();

        logger.info("Conversione completata con successo");
        return outputFile;
    }

    private String readJsonFile(File jsonFile) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n"); // Mantiene l'ordine originale
            }
            return content.toString();
        } catch (IOException e) {
            logger.error("File vuoto o corrotto");
            throw new IOException("File vuoto o corrotto");
        }
    }

    private Set<String> getKeyColumn(List<JSONObject> jsonObjects) {
        Set<String> colonne = new LinkedHashSet<>(); // Mantiene l'ordine originale
        for (JSONObject jsonObject : jsonObjects) {
            colonne.addAll(jsonObject.keySet());
        }
        return colonne;
    }


    private void writeJsonToSheet(List<JSONObject> jsonObjects, Sheet sheet, Workbook workbook, Set<String> colonne) throws IOException {
        if (jsonObjects.isEmpty()) {
            logger.error("Il file JSON è vuoto");
            throw new IOException("Il file è vuoto o corrotto");
        }

        Row headerRow = sheet.createRow(0);
        int colNum = 0;

        List<String> colonneList = new ArrayList<>();
        List<String> listAppoggio = new ArrayList<>(colonne); // Mantiene l'ordine originale
        for(int index = listAppoggio.size()-1; index >= 0; index--){
            colonneList.add(listAppoggio.get(index));
        }


        CellStyle headerStyle = createStyle(workbook, true);
        CellStyle rowStyle = createStyle(workbook, false);

        for (String key : colonneList) {
            Cell cell = headerRow.createCell(colNum++);
            cell.setCellValue(key);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;

        // Scrive i dati nell'ordine corretto
        for (JSONObject jsonObject : jsonObjects) { // Ora parte dal primo elemento e va fino all'ultimo
            Row row = sheet.createRow(rowNum);
            colNum = 0;

            for (String key : colonneList) {
                if (flagEndColumns[colNum]) { // Controlla se la colonna è ancora attiva
                    String value = jsonObject.optString(key, "");

                    if (value.isEmpty()) {
                        // Controlla se ci sono valori nelle righe successive
                        boolean hasValue = false;
                        for (int j = rowNum; j < jsonObjects.size(); j++) {
                            JSONObject nextObject = jsonObjects.get(j);
                            if (!nextObject.optString(key, "").isEmpty()) {
                                hasValue = true;
                                break;
                            }
                        }

                        if (!hasValue) {
                            flagEndColumns[colNum] = false; // Disattiva la colonna
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

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int colNum = 0; colNum < columnCount; colNum++) {
            sheet.autoSizeColumn(colNum);
        }
    }

    private CellStyle createStyle(Workbook workbook, boolean isHeader) {
        CellStyle style = workbook.createCellStyle();

        // Allineamento orizzontale e verticale
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Imposta il colore di sfondo
        if (isHeader) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        } else {
            style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        }
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
