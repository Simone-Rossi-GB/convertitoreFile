package Converters;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class XLStoJSONconverter implements Converter{
    private static final Logger logger = LogManager.getLogger(XLStoJSONconverter.class);
    @Override
    public ArrayList<File> convert(File xlsFile) throws IOException {
        logger.info("Conversione iniziata con parametri:\n | xlsFile.getPath() = {}", xlsFile.getPath());
        ArrayList<File> resultFiles = new ArrayList<>();

        try {
            File jsonFile = convertToJson(xlsFile);

            if (jsonFile.exists()) {
                resultFiles.add(jsonFile);
                logger.info("File convertito aggiunto alla lista: {}", jsonFile.getName());
            } else {
                logger.error("Conversione fallita: file JSON non creato correttamente");
            }

        } catch (Exception e) {
            logger.error("Errore durante la conversione: {}", e.getMessage());
            throw new IOException("Errore nella conversione XLS to JSON", e);
        }

        return resultFiles;
    }


    private File convertToJson(File xlsFile) throws IOException {
        // Usa nome file base e salva in src/temp/
        String baseName = xlsFile.getName().replaceFirst("[.][^.]+$", "");
        File outputDir = new File("src/temp");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(outputDir, baseName + ".json");
        return convertXlsToJson(xlsFile, outputFile.getAbsolutePath());
    }


    /**
     * Converte un file XLS in JSON
     *
     * @param xlsFile File XLS da convertire
     * @return File JSON creato
     * @throws IOException Se si verificano errori durante la conversione
     */
    private File convertXlsToJson(File xlsFile) throws IOException {
        // Genera automaticamente il percorso del file JSON
        String outputPath = generateJsonPath(xlsFile);
        return convertXlsToJson(xlsFile, outputPath);
    }

    /**
     * Converte un file XLS in JSON con percorso personalizzato
     *
     * @param xlsFile File XLS da convertire
     * @param outputPath Percorso del file JSON di output
     * @return File JSON creato
     * @throws IOException Se si verificano errori durante la conversione
     */
    private File convertXlsToJson(File xlsFile, String outputPath) throws IOException {
        File outputFile = new File(outputPath);

        try (InputStream fileStream = Files.newInputStream(xlsFile.toPath());
             Workbook workbook = new HSSFWorkbook(fileStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            List<String> headers = new ArrayList<>();
            List<Map<String, String>> dataList = new ArrayList<>();

            // Elabora le intestazioni (prima riga)
            if (rowIterator.hasNext()) {
                Row headerRow = rowIterator.next();
                for (Cell cell : headerRow) {
                    String headerValue = getCellValue(cell);
                    headers.add(headerValue.isEmpty() ? "column_" + cell.getColumnIndex() : headerValue);
                }
            }

            // Elabora i dati
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

            // Crea la cartella di output se necessario
            createOutputDirectory(outputFile);

            // Scrive il file JSON
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, dataList);

            logger.info("File creato: {} ({} bytes)", outputFile.getAbsolutePath(), outputFile.length());

            // Verifica che il file sia stato creato correttamente
            if (outputFile.exists() && outputFile.length() > 0) {
                return outputFile;
            } else {
                logger.error("Il file JSON non è stato creato correttamente");
                throw new IOException("Il file JSON non è stato creato correttamente");
            }

        } catch (Exception e) {
            // Pulizia in caso di errore
            cleanupFailedConversion(outputFile);
            logger.error("Errore durante la conversione XLS: {}", e.getMessage(), e);
            throw new IOException("Errore durante la conversione XLS: " + e.getMessage(), e);
        }
    }

    /**
     * Valida il file di input
     */
    private void validateInputFile(File inputFile) throws IOException {
        if (inputFile == null) {
            throw new IllegalArgumentException("Il file di input non può essere null");
        }

        if (!inputFile.exists()) {
            throw new FileNotFoundException("File non trovato: " + inputFile.getAbsolutePath());
        }

        if (!inputFile.isFile()) {
            throw new IllegalArgumentException("Il percorso non punta a un file: " + inputFile.getAbsolutePath());
        }

        if (!inputFile.canRead()) {
            throw new IOException("File non leggibile: " + inputFile.getAbsolutePath());
        }

        if (inputFile.length() == 0) {
            throw new IOException("Il file è vuoto: " + inputFile.getAbsolutePath());
        }
    }

    /**
     * Genera il percorso del file JSON sostituendo l'estensione
     */
    private String generateJsonPath(File inputFile) {
        String inputPath = inputFile.getAbsolutePath();
        return inputPath.replaceAll("\\.[^.]+$", ".json");
    }

    /**
     * Crea la cartella di output se non esiste
     */
    private void createOutputDirectory(File outputFile) throws IOException {
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean created = parentDir.mkdirs();
            if (!created) {
                throw new IOException("Impossibile creare la cartella: " + parentDir.getAbsolutePath());
            }
            System.out.println("  Cartella creata: " + parentDir.getAbsolutePath());
        }
    }

    /**
     * Pulisce i file creati in caso di conversione fallita
     */
    private void cleanupFailedConversion(File outputFile) {
        if (outputFile != null && outputFile.exists()) {
            boolean deleted = outputFile.delete();
            if (deleted) {
                System.out.println("  File parziale eliminato: " + outputFile.getName());
            }
        }
    }

    /**
     * Estrae il valore di una cella come stringa (senza usare getCellType)
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        // Prova con stringa
        try {
            String stringValue = cell.getStringCellValue();
            if (stringValue != null) {
                return stringValue.trim();
            }
        } catch (Exception e) {
            // Non è una stringa
        }

        // Prova con numero
        try {
            if (DateUtil.isCellDateFormatted(cell)) {
                Date dateValue = cell.getDateCellValue();
                return dateValue != null ? dateValue.toString() : "";
            } else {
                double numValue = cell.getNumericCellValue();
                if (numValue == Math.floor(numValue) && !Double.isInfinite(numValue)) {
                    return String.valueOf((long) numValue);
                }
                return Double.toString(numValue);
            }
        } catch (Exception e) {
            // Non è un numero
        }

        // Prova con booleano
        try {
            boolean boolValue = cell.getBooleanCellValue();
            return Boolean.toString(boolValue);
        } catch (Exception e) {
            // Non è un booleano
        }

        // Prova con formula
        try {
            String formula = cell.getCellFormula();
            if (formula != null && !formula.isEmpty()) {
                try {
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook()
                            .getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);

                    try {
                        return cellValue.getStringValue();
                    } catch (Exception ex1) {
                        try {
                            double numVal = cellValue.getNumberValue();
                            if (numVal == Math.floor(numVal)) {
                                return String.valueOf((long) numVal);
                            }
                            return Double.toString(numVal);
                        } catch (Exception ex2) {
                            try {
                                return Boolean.toString(cellValue.getBooleanValue());
                            } catch (Exception ex3) {
                                return formula;
                            }
                        }
                    }
                } catch (Exception evalEx) {
                    return formula;
                }
            }
        } catch (Exception e) {
            // Non è una formula
        }

        return "";
    }

    /**
     * Verifica se una riga è completamente vuota
     */
    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }

        for (Cell cell : row) {
            if (cell != null) {
                String cellValue = getCellValue(cell).trim();
                if (!cellValue.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Metodo di utilità per verificare se un file è supportato
     */
    public boolean isFileSupported(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".xls");
    }

    /**
     * Ottiene informazioni sul file di input
     */
    public String getFileInfo(File file) {
        if (file == null || !file.exists()) {
            return "File non valido";
        }

        return "Nome: " + file.getName() + "\n" +
                "Percorso: " + file.getAbsolutePath() + "\n" +
                "Dimensione: " + file.length() + " bytes\n" +
                "Leggibile: " + file.canRead() + "\n" +
                "Modificabile: " + file.canWrite() + "\n" +
                "Supportato: " + isFileSupported(file);
    }
}