package Converters;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.FileInputStream;
import java.io.IOException;

public class XlsToJson {
    public static void main(String[] args){
        String filePath = "src/main/java/Converters/prova.xls";

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // prima scheda
            JSONArray jsonArray = new JSONArray();

            // Prima riga: intestazioni
            Row headerRow = sheet.getRow(0);
            int numCols = headerRow.getLastCellNum();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                JSONObject jsonObject = new JSONObject();

                for (int c = 0; c < numCols; c++) {
                    Cell headerCell = headerRow.getCell(c);
                    Cell cell = row.getCell(c);

                    String key = headerCell != null ? headerCell.toString() : "Colonna" + c;
                    String value = cell != null ? cell.toString() : "";

                    jsonObject.put(key, value);
                }

                jsonArray.put(jsonObject);
            }

            System.out.println(jsonArray.toString(2)); // stampa il JSON con indentazione

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
