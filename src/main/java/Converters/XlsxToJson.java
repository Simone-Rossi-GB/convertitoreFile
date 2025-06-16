package Converters;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.IOException;

public class XlsxToJson {



    public static void main(String[] args) {
        String filePath = "src/main/java/Converters/prova.xls";  // Modifica con il tuo percorso

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {  // Usa XSSFWorkbook per .xlsx

            Sheet sheet = workbook.getSheetAt(0);          // Legge il primo foglio
            JSONArray jsonArray = new JSONArray();

            Row headerRow = sheet.getRow(0);               // Intestazioni nella prima riga
            int numCols = headerRow.getLastCellNum();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                JSONObject jsonObject = new JSONObject();

                for (int c = 0; c < numCols; c++) {
                    Cell headerCell = headerRow.getCell(c);
                    Cell cell = row.getCell(c);

                    String key = headerCell != null ? headerCell.toString() : "Col" + c;
                    String value = getCellValue(cell);

                    jsonObject.put(key, value);
                }

                jsonArray.put(jsonObject);
            }

            // Stampa o salva il JSON
            System.out.println(jsonArray.toString(2));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

}