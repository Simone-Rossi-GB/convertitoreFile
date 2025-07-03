package webService.server.converters.jsonCsvConverters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Cell;
import org.odftoolkit.simple.table.Table;
import webService.server.config.configHandlers.Config;
import webService.server.converters.Converter;

import java.io.*;

/**
 * Classe per la conversione di un file CSV in un file Spreadsheet (.xls/.xlsx/.ods).
 * Utilizza Apache POI per la gestione dei fogli di calcolo e OdfToolkit per ODS.
 * Applica uno stile personalizzato alle celle.
 */
public class CSVtoSPREADSHEETconverter extends Converter {
    public static final Logger logger = LogManager.getLogger(CSVtoSPREADSHEETconverter.class);

    @Override
    public File convert(File srcFile, Config configuration) throws Exception {
        logger.info("Percorso file da convertire: " + srcFile.getAbsolutePath());

        if (!srcFile.exists() || !srcFile.isFile()) {
            logger.error("Errore durante la conversione");
            throw new IllegalArgumentException("Il file è vuoto o corrotto.");
        }

        String format = configuration.getData().getDestinationFormat().toLowerCase();

        if (format.equals("ods")) {
            return odsWriter(srcFile);
        }

        Workbook workbook = formatSelector(format);
        return writeCsvToSheet(srcFile, workbook, format);
    }

    private Workbook formatSelector(String format) {
        switch (format) {
            case "xls": return new HSSFWorkbook();
            case "xlsx": return new XSSFWorkbook();
            default: throw new IllegalArgumentException("Formato non supportato da Apache POI: " + format);
        }
    }

    private File writeCsvToSheet(File csvFile, Workbook workbook, String format) throws IOException {
        Sheet sheet = workbook.createSheet("Dati");

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

                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(colNum++);
                    cell.setCellValue(value);
                    cell.setCellStyle(rowNum == 1 ? headerStyle : cellStyle);

                }

                colCount = Math.max(colCount, colNum);
            }

            for (int colNum = 0; colNum < colCount; colNum++) {
                sheet.autoSizeColumn(colNum);
            }
        }

        String outputFileName = csvFile.getName().replaceAll("\\.csv$", "") + "." + format;
        File outputFile = new File(csvFile.getParent(), outputFileName);

        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }

        workbook.close();
        logger.info("Percorso file convertito: " + outputFile.getAbsolutePath());
        return outputFile;
    }

    private File odsWriter(File csvFile) throws Exception {
        SpreadsheetDocument document = SpreadsheetDocument.newSpreadsheetDocument();
        Table sheet = document.getSheetByIndex(0);
        sheet.setTableName("Dati");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int rowNum = 0;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                for (int colNum = 0; colNum < values.length; colNum++) {
                    String value = values[colNum].trim();

                    if (value.startsWith("\"")) {
                        StringBuilder concatenatedValue = new StringBuilder(value);
                        while (!value.endsWith("\"") && colNum + 1 < values.length) {
                            value = values[++colNum].trim();
                            concatenatedValue.append(",").append(value);
                        }
                        value = concatenatedValue.toString();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                    }

                    Cell cell = sheet.getCellByPosition(colNum, rowNum);
                    cell.setStringValue(value);
                    styleOdsCell(cell, rowNum == 0);
                }
                rowNum++;
            }
        }

        String outputFileName = csvFile.getName().replaceAll("\\.csv$", "") + ".ods";
        File outputFile = new File(csvFile.getParent(), outputFileName);
        document.save(outputFile);
        logger.info("Percorso file ODS creato: " + outputFile.getAbsolutePath());
        return outputFile;
    }

    /**
     * Crea uno stile per le celle in formato Excel, con intestazioni evidenziate e bordi neri.
     */
    private CellStyle createStyle(Workbook workbook, boolean isHeader) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        if (isHeader) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

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

    /**
     * Applica sfondo grigio alle celle intestazione ODS.
     * Nota: OdfToolkit non supporta l'applicazione dei bordi visibili.
     *
     * @param cell     La cella da stilizzare
     * @param isHeader True se intestazione
     */
    private void styleOdsCell(Cell cell, boolean isHeader) {
        if (isHeader) {
            try {
                cell.setCellBackgroundColor("#D9D9D9"); // Grigio chiaro
            } catch (Exception e) {
                logger.warn("Impossibile applicare sfondo intestazione ODS: " + e.getMessage());
            }
        }
    }





}
