package Converters;

import Converters.exception.ConvertionException;
import com.itextpdf.text.DocumentException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.json.*;

import java.io.*;

public class JSONtoXLSconverter implements Converter {
    public static final Logger logger = LogManager.getLogger(JSONtoXLSconverter.class);

    @Override
    public File convert(File srcFile) throws IOException, DocumentException, ConvertionException {
        logger.info("Conversione iniziata con parametri:\n | srcFile.getPath() = {}", srcFile.getPath());

        if (controlloFileNonVuoto(srcFile)) {
            File validJsonFile = ensureJSONArrayFormat(srcFile);
            logger.info("File convertito correttamente");
            return convertJSONtoXLS(validJsonFile);
        } else {
            logger.error("File vuoto: {}", srcFile.getPath());
            throw new ConvertionException("File vuoto o corrotto");
        }
    }

    private boolean controlloFileNonVuoto(File srcFile) {
        return srcFile.length() > 0;
    }

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

    private File convertJSONtoXLS(File jsonFile) throws IOException {
        File fixedJsonFile = ensureJSONArrayFormat(jsonFile);
        String content = new String(java.nio.file.Files.readAllBytes(fixedJsonFile.toPath()));
        JSONArray jsonArray = new JSONArray(content);

        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sheet1");
        int rowIndex = 0;

        CellStyle headerStyle = createStyle(workbook, IndexedColors.BLUE, true, true);
        CellStyle keyStyle = createStyle(workbook, IndexedColors.GREY_25_PERCENT, false, true);
        CellStyle valueStyle = createStyle(workbook, IndexedColors.LIGHT_TURQUOISE, false, true);

        for (int i = 0; i < jsonArray.length(); i++) {
            Object item = jsonArray.get(i);
            if (item instanceof JSONObject) {
                rowIndex = writeJSONObject((JSONObject) item, sheet, rowIndex, headerStyle, keyStyle, valueStyle);
                rowIndex += 2;
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        File outDir = new File("output");
        if (!outDir.exists()) outDir.mkdirs();
        File xlsFile = new File(outDir, "converted.xls");
        try (FileOutputStream fos = new FileOutputStream(xlsFile)) {
            workbook.write(fos);
        }
        workbook.close();

        return xlsFile;
    }

    private int writeJSONObject(JSONObject obj, Sheet sheet, int rowIndex, CellStyle headerStyle, CellStyle keyStyle, CellStyle valueStyle) {
        for (String key : obj.keySet()) {
            Object value = obj.get(key);
            if (value instanceof JSONObject) {
                int height = countJsonPairs((JSONObject) value);

                Row r = sheet.createRow(rowIndex);
                Cell keyCell = r.createCell(0);
                keyCell.setCellValue(key);
                keyCell.setCellStyle(headerStyle);

                if (height > 1) {
                    CellRangeAddress newRegion = new CellRangeAddress(rowIndex, rowIndex + height - 1, 0, 0);
                    if (!isOverlappingMergedRegion(sheet, newRegion)) {
                        sheet.addMergedRegion(newRegion);
                    }
                }

                rowIndex = writeJSONObject((JSONObject) value, sheet, rowIndex, headerStyle, keyStyle, valueStyle);
            }
            else if (value instanceof JSONArray) {
                JSONArray arr = (JSONArray) value;

                Row r = sheet.createRow(rowIndex);
                Cell keyCell = r.createCell(0);
                keyCell.setCellValue(key);
                keyCell.setCellStyle(headerStyle);

                if (arr.length() > 1) {
                    CellRangeAddress newRegion = new CellRangeAddress(rowIndex, rowIndex + arr.length() - 1, 0, 0);
                    if (!isOverlappingMergedRegion(sheet, newRegion)) {
                        sheet.addMergedRegion(newRegion);
                    }
                }

                for (int i = 0; i < arr.length(); i++) {
                    Object arrVal = arr.get(i);
                    rowIndex = writeValue(arrVal, null, sheet, rowIndex, keyStyle, valueStyle);
                }

            } else {
                rowIndex = writeValue(value, key, sheet, rowIndex, keyStyle, valueStyle);
            }
        }
        return rowIndex;
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

    private int writeValue(Object value, String keyLabel, Sheet sheet, int rowIndex, CellStyle keyStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex);

        if (keyLabel != null) {
            Cell keyCell = row.createCell(0);
            keyCell.setCellValue(keyLabel);
            keyCell.setCellStyle(keyStyle);
        }

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(String.valueOf(value));
        valueCell.setCellStyle(valueStyle);

        return rowIndex + 1;
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
