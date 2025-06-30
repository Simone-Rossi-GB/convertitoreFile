package webService.server.converters.jsonCsvConverters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.table.Table;
import webService.server.configuration.configHandlers.conversionContext.ConversionContextReader;
import webService.server.converters.Converter;

import java.io.*;

/**
 * Classe per la conversione di un file CSV in un file Spreadsheet (.xls/.xlsx/.ods).
 * Utilizza Apache POI per la gestione dei fogli di calcolo e OdfToolkit per ODS.
 * Applica uno stile personalizzato alle celle.
 */
public class CSVtoSPREADSHEETconverter extends Converter {
    // Logger per la gestione degli errori e informazioni
    public static final Logger logger = LogManager.getLogger(CSVtoSPREADSHEETconverter.class);

    /**
     * Metodo principale per la conversione di un file CSV in Spreadsheet.
     *
     * @param srcFile Il file CSV da convertire.
     * @return Il file Spreadsheet generato.
     * @throws Exception Se il file non esiste o si verifica un errore durante la conversione.
     */
    @Override
    public File convert(File srcFile) throws Exception {
        logger.info("Percorso file da convertire: " + srcFile.getAbsolutePath());

        // Verifica l'esistenza e la validità del file sorgente
        if (!srcFile.exists() || !srcFile.isFile()) {
            logger.error("Errore durante la conversione");
            throw new IllegalArgumentException("Il file è vuoto o corrotto.");
        }

        // Ottiene il formato di destinazione richiesto
        String format = ConversionContextReader.getDestinationFormat().toLowerCase();

        // Se si tratta di ODS, utilizza un metodo dedicato
        if (format.equals("ods")) {
            return odsWriter(srcFile);
        }

        // Altrimenti procede con Apache POI
        Workbook workbook = formatSelector(format);
        return writeCsvToSheet(srcFile, workbook, format);
    }

    /**
     * Metodo che seleziona e restituisce un Workbook compatibile con Apache POI in base al formato.
     *
     * @param format Formato richiesto (xls o xlsx).
     * @return Workbook corrispondente.
     */
    private Workbook formatSelector(String format) {
        switch (format) {
            case "xls":
                return new HSSFWorkbook();
            case "xlsx":
                return new XSSFWorkbook();
            default:
                throw new IllegalArgumentException("Formato non supportato da Apache POI: " + format);
        }
    }

    /**
     * Converte un file CSV in un foglio di calcolo (xls o xlsx) utilizzando Apache POI.
     *
     * @param csvFile  Il file CSV da convertire.
     * @param workbook Il workbook POI dove scrivere i dati.
     * @param format   Formato di destinazione.
     * @return Il file Spreadsheet generato.
     * @throws IOException Se si verifica un errore durante la lettura o scrittura.
     */
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

                    // Gestione valori tra virgolette che includono virgole
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

                    Cell cell = row.createCell(colNum++);
                    cell.setCellValue(value);
                    cell.setCellStyle(rowNum == 1 ? headerStyle : cellStyle);
                }

                colCount = Math.max(colCount, colNum);
            }

            // Ridimensionamento automatico colonne
            for (int colNum = 0; colNum < colCount; colNum++) {
                sheet.autoSizeColumn(colNum);
            }
        }

        // Scrittura file di output
        String outputFileName = csvFile.getName().replaceAll("\\.csv$", "") + "." + format;
        File outputFile = new File(csvFile.getParent(), outputFileName);

        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }

        workbook.close();
        logger.info("Percorso file convertito: " + outputFile.getAbsolutePath());
        return outputFile;
    }

    /**
     * Metodo dedicato alla conversione CSV → ODS tramite OdfToolkit.
     *
     * @param csvFile Il file CSV da convertire.
     * @return Il file ODS generato.
     * @throws Exception Se si verifica un errore durante la generazione ODS.
     */
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
                    sheet.getCellByPosition(colNum, rowNum).setStringValue(value);
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
     * Crea uno stile per le celle, con intestazioni evidenziate.
     *
     * @param workbook Workbook in cui creare lo stile.
     * @param isHeader True se lo stile è per l’intestazione.
     * @return Lo stile cella configurato.
     */
    private CellStyle createStyle(Workbook workbook, boolean isHeader) {
        CellStyle style = workbook.createCellStyle();

        // Allineamento contenuti
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Colore intestazione
        if (isHeader) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        // Bordatura
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
