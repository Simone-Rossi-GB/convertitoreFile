package WebService.Converters;

import com.itextpdf.text.DocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.ArrayList;


public class JSONtoXLSconverter implements Converter {

    @Override
    public ArrayList<File> convert(File srcFile) throws IOException, DocumentException {
        return convertJSONtoXLS(srcFile);
    }


    private ArrayList<File> convertJSONtoXLS(File jsonFile) throws IOException {
        ArrayList<File> result = new ArrayList<>();

        try (InputStream is = new FileInputStream(jsonFile)) {
            JSONTokener tokener = new JSONTokener(is);
            JSONArray jsonArray = new JSONArray(tokener);

            Workbook workbook = new HSSFWorkbook(); // XLS format
            Sheet sheet = workbook.createSheet("Data");

            // Header
            if (jsonArray.length() > 0) {
                JSONObject first = jsonArray.getJSONObject(0);
                Row headerRow = sheet.createRow(0);
                int cellIndex = 0;
                for (String key : first.keySet()) {
                    Cell cell = headerRow.createCell(cellIndex++);
                    cell.setCellValue(key);
                }

                // Data rows
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    Row row = sheet.createRow(i + 1);
                    cellIndex = 0;
                    for (String key : first.keySet()) {
                        Cell cell = row.createCell(cellIndex++);
                        Object value = obj.opt(key);
                        if (value != null) {
                            cell.setCellValue(value.toString());
                        }
                    }
                }
            }

            // Output file
            String outputPath = jsonFile.getParent() + File.separator + removeExtension(jsonFile.getName()) + ".xls";
            File outFile = new File(outputPath);

            try (OutputStream os = new FileOutputStream(outFile)) {
                workbook.write(os);
            }

            workbook.close();
            result.add(outFile);
        }

        return result;
    }

    private String removeExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return (index > 0) ? filename.substring(0, index) : filename;
    }
}