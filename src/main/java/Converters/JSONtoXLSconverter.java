package Converters;

import com.itextpdf.text.DocumentException;
import converter.ConvertionException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

public class JSONtoXLSconverter implements Converter {

    @Override
    public ArrayList<File> convert(File srcFile) throws IOException, DocumentException, ConvertionException {
        if(controlloFileNonVuoto(srcFile)){
            File validJsonFile = ensureJSONArrayFormat(srcFile);
            return convertJSONtoXLS(validJsonFile);
        }
        throw new ConvertionException("File vuoto o corrotto");
    }

    private boolean controlloFileNonVuoto(File srcFile) {
        if(srcFile.length() == 0){
            return false;
        }
        return true;
    }


    /**
     * Controlla se il file JSON inizia e finisce con parentesi quadre.
     * Se no, le aggiunge e restituisce un nuovo file temporaneo formattato correttamente.
     */
    private File ensureJSONArrayFormat(File jsonFile) throws IOException {
        String content = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath())).trim();

        boolean startsWithBracket = content.startsWith("[");
        boolean endsWithBracket = content.endsWith("]");

        if (!startsWithBracket || !endsWithBracket) {
            // Rende il contenuto un array se non lo è
            content = "[" + content;
            if (!endsWithBracket) {
                content = content + "]";
            }

            // Scrive il contenuto corretto in un file temporaneo
            File tempFile = File.createTempFile("fixed-json-", ".json");
            try (FileWriter fw = new FileWriter(tempFile)) {
                fw.write(content);
            }

            return tempFile;
        }

        // Il file è già corretto
        return jsonFile;
    }

    private ArrayList<File> convertJSONtoXLS(File jsonFile) throws IOException {
        ArrayList<File> result = new ArrayList<>();
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("FoglioCalc");

        String jsonString = new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8);
        int currentRow = 0;
        int cellIndex = 0;
        Row row = sheet.createRow(currentRow++);
        StringBuilder currentToken = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes;
                if (!insideQuotes && currentToken.length() > 0) {
                    row.createCell(cellIndex++).setCellValue(currentToken.toString());
                    currentToken.setLength(0);
                }
            } else if (insideQuotes) {
                currentToken.append(c);
            } else if (c == ':' || c == ',') {
                continue; // skip separators outside quotes
            } else if (c == '{' || c == '[') {
                if (row.getPhysicalNumberOfCells() > 0) {
                    row = sheet.createRow(currentRow++);
                    cellIndex = 0;
                }
            } else if (c == '}' || c == ']') {
                if (currentToken.length() > 0) {
                    row.createCell(cellIndex++).setCellValue(currentToken.toString());
                    currentToken.setLength(0);
                }
                row = sheet.createRow(currentRow++);
                cellIndex = 0;
            }
        }

        for (int i = 0; i < 50; i++) {
            sheet.autoSizeColumn(i);
        }

        String outputPath = jsonFile.getParent() + File.separator + removeExtension(jsonFile.getName()) + ".xls";
        File outFile = new File(outputPath);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            workbook.write(fos);
        }

        workbook.close();
        result.add(outFile);
        return result;
    }

    private String removeExtension(String filename) {
        int index = filename.lastIndexOf('.');
        return (index > 0) ? filename.substring(0, index) : filename;
    }


}