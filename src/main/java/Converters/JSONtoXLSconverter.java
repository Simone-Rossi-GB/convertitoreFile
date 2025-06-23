package Converters;

import Converters.exception.ConversionException;
//import converter.ConvertionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.json.*;

import java.io.*;

public class JSONtoXLSconverter extends Converter {
    public static final Logger logger = LogManager.getLogger(JSONtoXLSconverter.class);

    @Override
    public File convert(File srcFile) throws Exception, ConversionException {
        logger.info("Conversione iniziata con parametri:\n | srcFile.getPath() = {}", srcFile.getPath());

        if (controlloFileNonVuoto(srcFile)) {
            File validJsonFile = ensureJSONArrayFormat(srcFile);
            logger.info("File convertito correttamente");
            return convertJSONtoXLS(validJsonFile, srcFile);
        } else {
            logger.error("File convertito alla lista: {}", srcFile.getPath());
            throw new ConversionException("File vuoto o corrotto");
        }
    }


    //controlla che il file non sia vuoto
    private boolean controlloFileNonVuoto(File srcFile) {
        return srcFile.length() > 0;
    }

    //va a controllare nel caso il json non abbia le quadre, gliele aggiunge e gestisce come array json
    private File ensureJSONArrayFormat(File jsonFile) throws IOException {
        String content = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath())).trim();
        boolean startsWithBracket = content.startsWith("[");
        boolean endsWithBracket = content.endsWith("]");

        if (!startsWithBracket || !endsWithBracket) {
            content = "[" + content;
            if (!endsWithBracket) {
                content += "]";
            }
            File tempFile = File.createTempFile("fixed-json-", ".json");
            try (FileWriter fw = new FileWriter(tempFile)) {
                fw.write(content);
            }
            return tempFile;
        }
        return jsonFile;
    }

    //conversione del file, richiama il metodo writeJSONObject per la scrittura sul foglio
    private File convertJSONtoXLS(File jsonFile, File srcFile) throws IOException {
        File result;
        String content = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath()));
        JSONArray jsonArray = new JSONArray(content);

        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("JSON");

        CellStyle blueStyle = createStyle(workbook, IndexedColors.BLUE, true, true); // chiavi oggetti annidati
        short customColorIndex = IndexedColors.LAVENDER.getIndex();
        ((HSSFWorkbook) workbook).getCustomPalette().setColorAtIndex(customColorIndex, (byte) 0x50, (byte) 0x4A, (byte) 0xAB);
        CellStyle lightBlueStyle = createStyle(workbook, IndexedColors.LAVENDER, false, true); // valori
        CellStyle datiStyle = createStyle(workbook, IndexedColors.GREY_25_PERCENT, true, true);
        CellStyle objectKeyStyle = createStyle(workbook, IndexedColors.WHITE, true, true); // oggetti contenitori

        int rowIndex = 0;
        boolean useBlue = true;

        for (int i = 0; i < jsonArray.length(); i++) {
            Object entry = jsonArray.get(i);

            if (entry instanceof JSONObject) {
                JSONObject obj = (JSONObject) entry;

                if (obj.isEmpty()) {
                    Row row = sheet.createRow(rowIndex);
                    Cell cell = row.createCell(0);
                    cell.setCellValue("DATI");
                    cell.setCellStyle(datiStyle);
                    CellRangeAddress newRegion = new CellRangeAddress(rowIndex, rowIndex, 0, 1);
                    if (!isOverlappingMergedRegion(sheet, newRegion)) {
                        sheet.addMergedRegion(newRegion);
                    }
                    rowIndex++;
                    continue;
                }

                rowIndex = writeJSONObject(obj, sheet, rowIndex, lightBlueStyle, objectKeyStyle, blueStyle, lightBlueStyle);
                rowIndex += 2; // spazio tra oggetti
                useBlue = !useBlue; // alterna colore
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);

        File outDir = new File("output");
        if (!outDir.exists()) outDir.mkdirs();
        // Usa il nome del file sorgente, cambiando solo estensione
        String baseName = srcFile.getName().replaceFirst("(?i)\\.json$", "");
        File xlsFile = new File(outDir, baseName + ".xls");
        try (FileOutputStream fos = new FileOutputStream(xlsFile)) {
            workbook.write(fos);
        }
        workbook.close();

        result = xlsFile;
        return result;
    }

    //scrittura sul foglio
    private int writeJSONObject(JSONObject obj, Sheet sheet, int rowIndex, CellStyle valueStyle, CellStyle keyStyle, CellStyle nestedKeyStyle, CellStyle nestedValueStyle) {
        int startRow = rowIndex;
        for (String key : obj.keySet()) {
            Object value = obj.get(key);

            if (value instanceof JSONObject) {
                int subStart = rowIndex;

                // Scrive chiave dell'oggetto contenitore
                Row row = sheet.createRow(rowIndex);
                Cell keyCell = row.createCell(0);
                keyCell.setCellValue(key);
                keyCell.setCellStyle(keyStyle);

                int localRowStart = rowIndex;
                int nestedRowIndex = rowIndex;
                int currentCol = 1;

                JSONObject nestedObj = (JSONObject) value;
                for (String nestedKey : nestedObj.keySet()) {
                    Object nestedVal = nestedObj.get(nestedKey);

                    if (nestedVal instanceof JSONObject) {
                        int deepStart = ++nestedRowIndex;
                        // Rimozione duplicato: la variabile 'deepObj' è già stata dichiarata sopra.
                        // Rimuoviamo il blocco duplicato

                        Row innerTitle = sheet.createRow(nestedRowIndex);
                        Cell innerKeyCell = innerTitle.createCell(currentCol);
                        innerKeyCell.setCellValue(nestedKey);
                        innerKeyCell.setCellStyle(keyStyle);

                        JSONObject deepObj = (JSONObject) nestedVal;
                        for (String deepKey : deepObj.keySet()) {
                            Row dataRow = sheet.createRow(++nestedRowIndex);
                            Cell k = dataRow.createCell(currentCol);
                            k.setCellValue(deepKey);
                            k.setCellStyle(nestedKeyStyle);

                            Cell v = dataRow.createCell(currentCol + 1);
                            v.setCellValue(String.valueOf(deepObj.get(deepKey)));
                            v.setCellStyle(nestedValueStyle);
                        }
                    } else {
                        Row dataRow = sheet.createRow(++nestedRowIndex);
                        Cell k = dataRow.createCell(currentCol);
                        k.setCellValue(nestedKey);
                        k.setCellStyle(valueStyle);

                        Cell v = dataRow.createCell(currentCol + 1);
                        v.setCellValue(String.valueOf(nestedVal));
                        v.setCellStyle(valueStyle);
                    }
                }

                // Unione verticale della cella contenente il nome dell'oggetto contenitore
                if (nestedRowIndex > localRowStart) {
                    CellRangeAddress mergeRegion = new CellRangeAddress(localRowStart, nestedRowIndex, 0, 0);
                    if (!isOverlappingMergedRegion(sheet, mergeRegion)) {
                        sheet.addMergedRegion(mergeRegion);
                    }
                }

                rowIndex = nestedRowIndex + 1;

            } else if (value instanceof JSONArray) {
                Row row = sheet.createRow(rowIndex);
                Cell keyCell = row.createCell(0);
                keyCell.setCellValue(key);
                keyCell.setCellStyle(keyStyle);
                rowIndex++;

                JSONArray arr = (JSONArray) value;
                for (int i = 0; i < arr.length(); i++) {
                    Object item = arr.get(i);
                    if (item instanceof JSONObject) {
                        rowIndex = writeJSONObject((JSONObject) item, sheet, rowIndex, valueStyle, keyStyle, keyCell.getCellStyle(), keyCell.getCellStyle());
                    } else {
                        Row r = sheet.createRow(rowIndex);
                        Cell valCell = r.createCell(1);
                        valCell.setCellValue(String.valueOf(item));
                        valCell.setCellStyle(valueStyle);
                        rowIndex++;
                    }
                }

            } else {
                Row row = sheet.createRow(rowIndex);
                Cell keyCell = row.createCell(0);
                keyCell.setCellValue(key);
                keyCell.setCellStyle(keyStyle);

                Cell valCell = row.createCell(1);
                valCell.setCellValue(String.valueOf(value));
                valCell.setCellStyle(valueStyle);
                rowIndex++;
            }
        }
        return rowIndex;
    }

    private int countJsonPairs(JSONObject obj) {
        int count = 0;
        for (String k : obj.keySet()) {
            Object val = obj.get(k);
            if (val instanceof JSONObject) count += countJsonPairs((JSONObject) val);
            else if (val instanceof JSONArray) count += ((JSONArray) val).length();
            else count++;
        }
        return Math.max(count, 1);
    }

    private boolean isOverlappingMergedRegion(Sheet sheet, CellRangeAddress newRegion) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress existing = sheet.getMergedRegion(i);
            if (existing.intersects(newRegion)) {
                return true;
            }
        }
        return false;
    }

    private CellStyle createStyle(Workbook wb, IndexedColors bgColor, boolean bold, boolean border) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(bgColor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        if (border) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.WHITE.getIndex());
            style.setBottomBorderColor(IndexedColors.WHITE.getIndex());
            style.setLeftBorderColor(IndexedColors.WHITE.getIndex());
            style.setRightBorderColor(IndexedColors.WHITE.getIndex());
        }

        Font font = wb.createFont();
        font.setBold(bold);
        font.setColor((bgColor == IndexedColors.BLUE) ? IndexedColors.WHITE.getIndex() : IndexedColors.BLACK.getIndex());
        style.setFont(font);

        return style;
    }
}

