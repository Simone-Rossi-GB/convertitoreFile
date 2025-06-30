package webService.server.converters.jsonCsvConverters;

import org.odftoolkit.odfdom.type.Color;
import webService.server.configuration.configHandlers.conversionContext.ConversionContextReader;
import webService.server.converters.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Table;

import java.io.*;
import java.util.*;

/**
 * Converte un file JSON in un file di tipo Spreadsheet (.xls, .xlsx o .ods).
 * Supporta Apache POI per i formati Microsoft Excel e OdfToolkit per il formato OpenDocument.
 * Mantiene l'ordine delle colonne in base al primo oggetto JSON e rimuove le colonne completamente vuote.
 */
public class JSONtoSPREADSHEETconverter extends Converter {
    public static final Logger logger = LogManager.getLogger(JSONtoSPREADSHEETconverter.class);
    private boolean[] flagEndColumns;

    @Override
    public File convert(File srcFile) throws IOException {
        if (!srcFile.exists() || !srcFile.isFile()) {
            logger.error("File vuoto o corrotto");
            throw new IOException("Il file è vuoto o corrotto");
        }

        String jsonContent = readJsonFile(srcFile);
        JSONArray jsonArray = new JSONArray(jsonContent);
        List<JSONObject> jsonObjects = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            jsonObjects.add(jsonArray.getJSONObject(i));
        }

        String format = ConversionContextReader.getDestinationFormat().toLowerCase();

        if (format.equals("ods")) {
            try {
                return writeJsonToOds(jsonObjects, srcFile);
            } catch (Exception e) {
                logger.error("Errore durante la generazione ODS", e);
                throw new IOException("Errore durante la generazione ODS: " + e.getMessage(), e);
            }
        }

        Workbook workbook = selectWorkbook(format);
        Sheet sheet = workbook.createSheet("Dati");

        List<String> colonneList = getOrderedColumns(jsonObjects);
        flagEndColumns = new boolean[colonneList.size()];
        Arrays.fill(flagEndColumns, true);

        writeJsonToSheet(jsonObjects, sheet, workbook, colonneList);
        autoSizeColumns(sheet, colonneList.size());

        String outputFileName = srcFile.getName().replaceAll("\\.json$", "") + "." + format;
        File outputFile = new File(srcFile.getParent(), outputFileName);

        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }
        workbook.close();

        logger.info("Conversione completata con successo");
        return outputFile;
    }

    private Workbook selectWorkbook(String format) {
        switch (format) {
            case "xls":
                return new HSSFWorkbook();
            case "xlsx":
                return new XSSFWorkbook();
            default:
                throw new IllegalArgumentException("Formato non supportato da Apache POI: " + format);
        }
    }

    private File writeJsonToOds(List<JSONObject> jsonObjects, File srcFile) throws Exception {
        SpreadsheetDocument document = SpreadsheetDocument.newSpreadsheetDocument();
        Table sheet = document.getSheetByIndex(0);
        sheet.setTableName("Dati");

        List<String> colonneList = getOrderedColumns(jsonObjects);

        for (int col = 0; col < colonneList.size(); col++) {
            Cell headerCell = sheet.getCellByPosition(col, 0);
            headerCell.setStringValue(colonneList.get(col));
            styleOdsCell(headerCell, true);
        }

        for (int row = 0; row < jsonObjects.size(); row++) {
            JSONObject obj = jsonObjects.get(row);
            for (int col = 0; col < colonneList.size(); col++) {
                String key = colonneList.get(col);
                String value = obj.optString(key, "");
                Cell dataCell = sheet.getCellByPosition(col, row + 1);
                dataCell.setStringValue(value);
                styleOdsCell(dataCell, false);
            }
        }

        String outputFileName = srcFile.getName().replaceAll("\\.json$", "") + ".ods";
        File outputFile = new File(srcFile.getParent(), outputFileName);
        document.save(outputFile);
        return outputFile;
    }

    private String readJsonFile(File jsonFile) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    private List<String> getOrderedColumns(List<JSONObject> jsonObjects) {
        if (jsonObjects.isEmpty()) return new ArrayList<>();
        JSONObject first = jsonObjects.get(0);
        return new ArrayList<>(first.keySet());
    }

    private void writeJsonToSheet(List<JSONObject> jsonObjects, Sheet sheet, Workbook workbook, List<String> colonneList) throws IOException {
        if (jsonObjects.isEmpty()) {
            logger.error("Il file JSON è vuoto");
            throw new IOException("Il file è vuoto o corrotto");
        }

        Row headerRow = sheet.createRow(0);
        int colNum = 0;

        CellStyle headerStyle = createStyle(workbook, true);

        for (String key : colonneList) {
            Cell cell = (Cell) headerRow.createCell(colNum++);
            cell.setStringValue(key);
            cell.setCellBackgroundColor((Color) headerStyle);
        }

        int rowNum = 1;

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
                        Cell cell = (Cell) row.createCell(colNum);
                        cell.setStringValue(key);
                        cell.setCellBackgroundColor((Color) headerStyle);
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
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(isHeader ? IndexedColors.GREY_25_PERCENT.getIndex() : IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

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

    /**
     * Applica uno sfondo grigio alle celle ODS (solo intestazioni).
     * I bordi non sono supportati da OdfToolkit in modo affidabile.
     *
     * @param cell     Cella ODS da modificare
     * @param isHeader True se si tratta di intestazione
     */
    private void styleOdsCell(Cell cell, boolean isHeader) {
        if (isHeader) {
            try {
                cell.setCellBackgroundColor("#D9D9D9"); // Simile a GREY_25_PERCENT
            } catch (Exception e) {
                logger.warn("Errore applicando sfondo intestazione ODS: " + e.getMessage());
            }
        }
    }
}
