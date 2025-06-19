package Converters;

import com.itextpdf.text.DocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.util.ArrayList;

public class JSONtoXLSconverter implements Converter {

    @Override
    public ArrayList<File> convert(File srcFile) throws IOException, DocumentException {
        File validJsonFile = ensureJSONArrayFormat(srcFile);
        return convertJSONtoXLS(validJsonFile);
    }

    @Override
    public ArrayList<File> convert(File srcFile, String password) throws IOException, DocumentException {
        File validJsonFile = ensureJSONArrayFormat(srcFile);
        return convertJSONtoXLS(validJsonFile);
    }

    @Override
    public ArrayList<File> convert(File srcFile, boolean opzioni) throws IOException, DocumentException {
        File validJsonFile = ensureJSONArrayFormat(srcFile);
        return convertJSONtoXLS(validJsonFile);
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

        try (InputStream is = new FileInputStream(jsonFile)) {
            JSONTokener tokener = new JSONTokener(is);
            JSONArray jsonArray = new JSONArray(tokener);

            Workbook workbook = new HSSFWorkbook(); // formato XLS
            Sheet sheet = workbook.createSheet("Data");

            if (jsonArray.length() > 0) {
                JSONObject first = jsonArray.getJSONObject(0);
                Row headerRow = sheet.createRow(0);
                int cellIndex = 0;

                for (String key : first.keySet()) {
                    Cell cell = headerRow.createCell(cellIndex++);
                    cell.setCellValue(key);
                }

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