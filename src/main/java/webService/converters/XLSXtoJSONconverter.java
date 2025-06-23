package webService.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class XLSXtoJSONconverter implements Converter {
    private static final Logger logger = LogManager.getLogger(ZIPtoTARGZconverter.class);
    /**
     * Converte un file .xlsx in un file .json
     * @param excelFile il file Excel (.xlsx) in input
     * @return un file JSON generato con i dati dell'Excel
     * @throws IOException in caso di errori di lettura/scrittura
     */
    public File convertXlsxToJson(File excelFile) throws IOException {
        // Carica il file Excel
        try (InputStream inputStream = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0); // Primo foglio
            Iterator<Row> rowIterator = sheet.iterator();

            List<Map<String, String>> jsonData = new ArrayList<>();
            List<String> headers = new ArrayList<>();

            // Intestazioni (prima riga)
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                for (Cell cell : headerRow) {
                    headers.add(getCellValue(cell));
                }
            }

            // Righe dati
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowMap = new LinkedHashMap<>();

                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowMap.put(headers.get(i), getCellValue(cell));
                }

                jsonData.add(rowMap);
            }

            // Crea file temporaneo per l'output JSON
            File jsonFile = File.createTempFile("excel-to-json-", ".json");

            // Scrittura del JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, jsonData);

            return jsonFile;
        }
    }

    /**
     * Estrae il valore della cella come stringa
     */
    private String getCellValue(Cell cell) {
        CellType cellType = cell.getCellType();

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return Double.toString(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    @Override
    public ArrayList<File> convert(File srcFile) throws IOException {
        logger.info("Conversione iniziata con parametri:\n | srcFile.getPath() = {}", srcFile.getPath());
        ArrayList<File> resultFiles = new ArrayList<>();
        File jsonFile = convertXlsxToJson(srcFile);
        resultFiles.add(jsonFile);
        return resultFiles;
    }

}
