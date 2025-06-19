package Converters;

import converter.ConvertionException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.*;

public class XLStoJSONconverter implements Converter {
    @Override
    public ArrayList<File> convert(File xlsFile) throws IOException, ConvertionException {
        ArrayList<File> resultFiles = new ArrayList<>();

        // Controlla se il file XLS contiene dati
        if (!controllaFileNonVuoto(xlsFile)) {
            throw new ConvertionException("File vuoto o corrotto");
        }
        try {
            File jsonFile = convertToJson(xlsFile);

            if (jsonFile != null && jsonFile.exists()) {
                resultFiles.add(jsonFile);
                System.out.println("File convertito aggiunto alla lista: " + jsonFile.getName());
            } else {
                System.err.println("Conversione fallita: file JSON non creato correttamente");
            }

        } catch (Exception e) {
            System.err.println("Errore durante la conversione: " + e.getMessage());
            throw new IOException("Errore nella conversione XLS to JSON", e);
        }

        return resultFiles;
    }

    private File convertToJson(File xlsFile) throws IOException {
        String baseName = xlsFile.getName().replaceFirst("[.][^.]+$", "");
        File outputDir = new File("src/temp");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(outputDir, baseName + ".json");
        return convertXlsToJson(xlsFile, outputFile.getAbsolutePath());
    }

    private File convertXlsToJson(File xlsFile, String outputPath) throws IOException {
        File outputFile = new File(outputPath);

        System.out.println("Conversione XLS → JSON:");
        System.out.println("  Input: " + xlsFile.getAbsolutePath());
        System.out.println("  Output: " + outputFile.getAbsolutePath());

        try (InputStream fileStream = new FileInputStream(xlsFile);
             Workbook workbook = new HSSFWorkbook(fileStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            List<String> headers = new ArrayList<>();
            List<Map<String, String>> dataList = new ArrayList<>();

            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                for (Cell cell : headerRow) {
                    String headerValue = getCellValue(cell);
                    headers.add(headerValue.isEmpty() ? "column_" + cell.getColumnIndex() : headerValue);
                }
                System.out.println("  Intestazioni: " + headers);
            }

            int rowCount = 0;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                if (isRowEmpty(row)) {
                    continue;
                }

                Map<String, String> rowData = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rowData.put(headers.get(i), getCellValue(cell));
                }

                dataList.add(rowData);
                rowCount++;
            }


            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, dataList);

            System.out.println("Conversione completata! Righe elaborate: " + rowCount);
            System.out.println("File creato: " + outputFile.getAbsolutePath() + " (" + outputFile.length() + " bytes)");

            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile;
            } else {
                throw new IOException("Il file JSON non è stato creato correttamente");
            }

        } catch (Exception e) {
            throw new IOException("Errore durante la conversione XLS: " + e.getMessage(), e);
        }
    }

    public boolean controllaFileNonVuoto(File xlsFile) {
        try (InputStream fileStream = new FileInputStream(xlsFile);
             Workbook workbook = new HSSFWorkbook(fileStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) {
                rowIterator.next(); // salta intestazione
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (!isRowEmpty(row)) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Errore durante il controllo del file: " + e.getMessage());
        }
        return false;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (cell != null && !getCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        try {
            return cell.getStringCellValue().trim();
        } catch (Exception ignored) {}
        try {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue().toString();
            } else {
                double val = cell.getNumericCellValue();
                return val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            }
        } catch (Exception ignored) {}
        try {
            return String.valueOf(cell.getBooleanCellValue());
        } catch (Exception ignored) {}
        try {
            return cell.getCellFormula();
        } catch (Exception ignored) {}
        return "";
    }
}