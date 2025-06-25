package converters.jsonCsvConverters;

import configuration.configHandlers.conversionContext.ConversionContextReader;
import converters.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class CSVtoSPREADSHEETconverter extends Converter {
    public static final Logger logger = LogManager.getLogger(CSVtoSPREADSHEETconverter.class);
    private final TreeMap<String, Short> coloriCelle = new TreeMap<>();
    private boolean reverseOrder = true;
    private String mapFollower;
    private int cunterBackWriting = 0;

    private void colorBuilding() {
        // Popola la mappa coloriCelle con i valori forniti
        coloriCelle.put("06065D", IndexedColors.BLUE.getIndex());
        coloriCelle.put("0504AA", IndexedColors.INDIGO.getIndex());
        coloriCelle.put("2337C6", IndexedColors.BLUE_GREY.getIndex());
        coloriCelle.put("395A7F", IndexedColors.GREY_40_PERCENT.getIndex());
        coloriCelle.put("4169E1", IndexedColors.ROYAL_BLUE.getIndex());
        coloriCelle.put("0A9396", IndexedColors.TEAL.getIndex());
        coloriCelle.put("90E0EF", IndexedColors.LIGHT_BLUE.getIndex());
        coloriCelle.put("C4C4DF", IndexedColors.LAVENDER.getIndex());
        coloriCelle.put("FFFFFF", IndexedColors.WHITE.getIndex());
    }

    public int getCunterBackWriting() { return cunterBackWriting; }
    public void setCunterBackWriting(int cunterBackWriting) { this.cunterBackWriting = cunterBackWriting; }

    @Override
    public File convert(File srcFile) throws Exception {
        colorBuilding();
        if (!srcFile.exists() || !srcFile.isFile()) {
            logger.error("Errore durante la conversione");
            throw new IllegalArgumentException("Il file vuoto o corrotto.");
        }
        return srcFile = writeCsvToSheet(srcFile);
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

            while ((line = br.readLine()) != null) {

                System.out.println("richiamo updateMapFollower");

                // Aggiorna il colore per la riga corrente
                updateMapFollower();

                System.out.println("fine richiamo updateMapFollower");

                // Crea una nuova riga nel foglio Excel
                Row row = sheet.createRow(rowNum++);

                // Dividi la riga del CSV in celle usando la virgola come delimitatore
                String[] values = line.split(",");
                int colNum = 0;

                for (int i = 0; i < values.length; i++) {
                    String value = values[i].trim();

                    // Controlla se il valore inizia con una virgoletta alta
                    if (value.startsWith("\"")) {
                        StringBuilder concatenatedValue = new StringBuilder(value);

                        // Continua a concatenare finché non trovi un valore che termina con una virgoletta alta
                        while (!value.endsWith("\"") && i + 1 < values.length) {
                            value = values[++i].trim();
                            concatenatedValue.append(",").append(value);
                        }

                        // Rimuovi le virgolette alte all'inizio e alla fine
                        value = concatenatedValue.toString();
                        if (value.startsWith("\"") && value.endsWith("\"")) {
                            value = value.substring(1, value.length() - 1);
                        }
                    }

                    // Crea lo stile della cella con il colore aggiornato
                    CellStyle cellStyle = workbook.createCellStyle();
                    cellStyle.setAlignment(HorizontalAlignment.CENTER);
                    cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                    cellStyle.setFillForegroundColor(coloriCelle.get(mapFollower));
                    cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                    // Scrivi il valore nella cella
                    Cell cell = row.createCell(colNum++);
                    cell.setCellValue(value);
                    cell.setCellStyle(cellStyle); // Applica lo stile con colore
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


    private void updateMapFollower() {
        // Ottieni l'insieme ordinato delle chiavi
        List<String> keyList = new ArrayList<>(coloriCelle.keySet()); // Converti in una lista per accedere agli indici

        // Controlla i limiti per evitare ripetizioni
        if (getCunterBackWriting() >= keyList.size()) {
            setCunterBackWriting(keyList.size() - 1); // Usa l'ultimo colore
            reverseOrder = false; // Inizia a iterare al contrario
        } else if (getCunterBackWriting() <= 0) {
            reverseOrder = true; // Torna a iterare in avanti
            setCunterBackWriting(0); // Evita valori negativi
        }

        // Aggiorna mapFollower con la nuova chiave
        mapFollower = keyList.get(getCunterBackWriting());

        // Modifica l'indice per la prossima iterazione
        if (reverseOrder) {
            setCunterBackWriting(getCunterBackWriting() + 1); // Vai avanti
            if (getCunterBackWriting() >= keyList.size()) {
                reverseOrder = false;
                setCunterBackWriting(keyList.size() - 2); // Torna indietro di uno
            }
        } else {
            setCunterBackWriting(getCunterBackWriting() - 1); // Vai indietro
            if (getCunterBackWriting() < 0) {
                reverseOrder = true;
                setCunterBackWriting(1); // Vai avanti di uno
            }
        }
    }



}
