package webService.server.converters.jsonCsvConverters;
//CSV - EXCEL

import webService.server.configuration.configHandlers.conversionContext.ConversionContextReader;
import webService.server.converters.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.*;

/**
 * Classe per la conversione di un file CSV in un file Spreadsheet (.xls/.xlsx/.ods).
 * Utilizza Apache POI per la gestione dei fogli di calcolo e applica uno stile personalizzato alle celle.
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
        // Controlla se il file esiste e non è corrotto
        if (!srcFile.exists() || !srcFile.isFile()) {
            logger.error("Errore durante la conversione");
            throw new IllegalArgumentException("Il file è vuoto o corrotto.");
        }

        // Avvia la conversione del file CSV in Spreadsheet
        return writeCsvToSheet(srcFile);
    }

    /**
     * Converte un file CSV in un foglio Excel.
     *
     * @param csvFile Il file CSV da convertire.
     * @return Il file Spreadsheet generato.
     * @throws IOException Se si verifica un errore durante la lettura o scrittura del file.
     */
    private File writeCsvToSheet(File csvFile) throws IOException {
        // Crea un nuovo Workbook
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Dati");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) { // Legge il file CSV riga per riga
            String line;
            int rowNum = 0; // Contatore delle righe
            int colCount = 0; // Contatore delle colonne

            // Crea gli stili per le celle
            CellStyle headerStyle = createStyle(workbook, true);
            CellStyle cellStyle = createStyle(workbook, false);

            while ((line = br.readLine()) != null) {
                Row row = sheet.createRow(rowNum++); // Crea una nuova riga nel foglio Excel
                String[] values = line.split(","); // Divide la riga CSV in valori separati da virgola
                int colNum = 0; // Contatore delle colonne

                for (int i = 0; i < values.length; i++) {
                    String value = values[i].trim(); // Rimuove spazi inutili

                    // Gestisce valori tra virgolette che contengono virgole
                    if (value.startsWith("\"")) {
                        StringBuilder concatenatedValue = new StringBuilder(value);
                        while (!value.endsWith("\"") && i + 1 < values.length) {
                            value = values[++i].trim();
                            concatenatedValue.append(",").append(value);
                        }
                        value = concatenatedValue.toString();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1); // Rimuove le virgolette
                        }
                    }

                    // Crea una cella e assegna il valore
                    Cell cell = row.createCell(colNum++);
                    cell.setCellValue(value);
                    cell.setCellStyle(rowNum == 1 ? headerStyle : cellStyle); // Applica lo stile corretto
                }

                // Aggiorna il numero massimo di colonne
                colCount = Math.max(colCount, colNum);
            }

            // Ridimensiona automaticamente le colonne
            for (int colNum = 0; colNum < colCount; colNum++) {
                sheet.autoSizeColumn(colNum);
            }
        } catch (IOException e) {
            logger.error("Errore durante la conversione");
            throw new IOException("Errore durante conversione: " + e.getMessage(), e);
        }

        // Genera il nome del file di output
        String outputFileName = csvFile.getName().replaceAll("\\.csv$", "") + "." + ConversionContextReader.getDestinationFormat();
        File outputFile = new File(csvFile.getParent(), outputFileName);

        // Scrive il file Excel
        try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
            workbook.write(fileOut);
        }

        workbook.close();
        logger.info("Percorso file convertito: " + outputFile.getAbsolutePath());
        return outputFile;
    }

    /**
     * Crea uno stile per le celle, con intestazioni colorate.
     *
     * @param workbook Workbook in cui applicare lo stile.
     * @param isHeader True se lo stile è per l'intestazione, false per le celle normali.
     * @return Lo stile creato.
     */
    private CellStyle createStyle(Workbook workbook, boolean isHeader) {
        CellStyle style = workbook.createCellStyle();

        // Allineamento orizzontale e verticale
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Imposta il colore di sfondo per le intestazioni
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