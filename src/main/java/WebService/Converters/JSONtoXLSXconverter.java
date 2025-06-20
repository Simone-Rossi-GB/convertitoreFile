package WebService.Converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.DocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class JSONtoXLSXconverter implements Converter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ArrayList<File> convert(File srcFile) throws IOException, DocumentException {
        return convertInternal(srcFile);
    }



    private ArrayList<File> convertInternal(File jsonFile) throws IOException {
        List<Map<String, Object>> data = objectMapper.readValue(
                jsonFile, new TypeReference<List<Map<String, Object>>>() {}
        );

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        // Write header
        if (!data.isEmpty()) {
            Row headerRow = sheet.createRow(0);
            Map<String, Object> firstRow = data.get(0);
            int cellIndex = 0;
            for (String key : firstRow.keySet()) {
                Cell cell = headerRow.createCell(cellIndex++);
                cell.setCellValue(key);
            }

            // Write data rows
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> rowData = data.get(i);
                int ci = 0;
                for (String key : firstRow.keySet()) {
                    Cell cell = row.createCell(ci++);
                    Object value = rowData.get(key);
                    cell.setCellValue(value != null ? value.toString() : "");
                }
            }
        }

        // Auto size columns (optional)
        for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
            sheet.autoSizeColumn(i);
        }

        // Output file
        File outFile = File.createTempFile("converted-", ".xlsx");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            workbook.write(fos);
        }
        workbook.close();

        ArrayList<File> result = new ArrayList<>();
        result.add(outFile);
        return result;
    }
}

