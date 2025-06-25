package converters.jsonCsvConverters;

import configuration.configHandlers.conversionContext.ConversionContextReader;
import converters.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.*;

public class CSVtoSPREADSHEETconverter extends Converter {
    public static final Logger logger = LogManager.getLogger(CSVtoSPREADSHEETconverter.class);

    @Override
    public File convert(File srcFile) throws Exception {
        if (!srcFile.exists() || !srcFile.isFile()) {
            logger.error("Errore durante la conversione");
            throw new IllegalArgumentException("Il file vuoto o corrotto.");
        }
        return writeCsvToSheet(srcFile);
    }

    private File writeCsvToSheet(File csvFile) throws IOException {
        // Crea un workbook per il file Excel
        Workbook workbook = new HSSFWorkbook(); // Formato .xls
        Sheet sheet = workbook.createSheet("Dati");

        // Leggi il file CSV e scrivi i dati nel foglio Excel
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int rowNum = 0;
            int colCount = 0;

            CellStyle headerStyle = createStyle(workbook, true);
            CellStyle cellStyle = createStyle(workbook, false);

            while ((line = br.readLine()) != null) {
                Row row = sheet.createRow(rowNum++);
                String[] values = line.split(",");
                int colNum = 0;

                for (int i = 0; i < values.length; i++) {
                    String value = values[i].trim();

                    // Gestione delle virgolette alte
                    if (value.startsWith("\"")) {
                        StringBuilder concatenatedValue = new StringBuilder(value);
                        while (!value.endsWith("\"") && i + 1 < values.length) {
                            value = values[++i].trim();
                            concatenatedValue.append(",").append(value);
                        }
                        value = concatenatedValue.toString();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                    }

                    // Crea la cella e applica lo stile
                    Cell cell = row.createCell(colNum++);
                    cell.setCellValue(value);
                    cell.setCellStyle(rowNum == 1 ? headerStyle : cellStyle); // Intestazione colorata
                }

                // Aggiorna il numero massimo di colonne
                colCount = Math.max(colCount, colNum);
            }

            // Autoridimensionamento delle colonne
            for (int colNum = 0; colNum < colCount; colNum++) {
                sheet.autoSizeColumn(colNum);
            }
        } catch (IOException e) {
            logger.error("Errore durante la conversione");
            throw new IOException("Errore durante conversione: " + e.getMessage(), e);
        }

        // Genera il nome del file di output con la stessa base del file CSV
        String outputFileName = csvFile.getName().replaceAll("\\.csv$", "") + "." + ConversionContextReader.getDestinationFormat();
        File outputFile = new File(csvFile.getParent(), outputFileName);

        // Salva il file Excel
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }

        // Chiudi il workbook
        workbook.close();
        return outputFile;
    }

    private CellStyle createStyle(Workbook workbook, boolean isHeader) {
        CellStyle style = workbook.createCellStyle();

        // Allineamento orizzontale e verticale
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Imposta il colore di sfondo per l'intestazione
        if (isHeader) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        // Aggiunta dei bordi neri su tutti i lati
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());

        return style;
    }
}
