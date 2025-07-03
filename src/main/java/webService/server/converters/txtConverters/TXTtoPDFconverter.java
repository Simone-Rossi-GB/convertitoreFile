package webService.server.converters.txtConverters;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import webService.server.config.configHandlers.conversionContext.ConversionContextReader;
import webService.server.converters.Converter;
import webService.server.converters.PDFWatermarkApplier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import webService.server.converters.exception.WatermarkException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TXTtoPDFconverter extends Converter {
    private static final Logger logger = LogManager.getLogger(TXTtoPDFconverter.class);

    /**
     * Conversione txt -> pdf con supporto watermark
     * @param srcFile file di partenza
     * @return file convertito (con watermark se specificato)
     * @throws IOException errori di lettura/scrittura sul file
     * @throws DocumentException errori nella creazione del PDF
     * @throws WatermarkException errori nell'applicazione del watermark
     */
    @Override
    public File convert(File srcFile) throws IOException, DocumentException, WatermarkException {
        logger.info("Conversione iniziata con parametri:\n | srcFile.getPath() = {}", srcFile.getPath());

        File output = new File(srcFile.getAbsolutePath().replaceAll("\\.txt$", ".pdf"));

        // Crea un nuovo documento pdf
        Document document = new Document();
        PdfWriter.getInstance(document, Files.newOutputStream(output.toPath()));
        document.open();

        try (BufferedReader reader = new BufferedReader(new FileReader(srcFile))) {
            String line;
            // Per ogni riga aggiunge al pdf un paragrafo
            while ((line = reader.readLine()) != null) {
                document.add(new Paragraph(line));
            }
        }
        document.close();

        // Applica watermark se presente
        if (!ConversionContextReader.getWatermark().isEmpty()) {
            logger.info("Applying watermark to PDF...");

            // Crea un file temporaneo per il PDF con watermark nella stessa directory
            File tempFile = new File(output.getParent(), "watermarked_" + output.getName());

            logger.info("Original file: {}", output.getAbsolutePath());
            logger.info("Temp file for watermark: {}", tempFile.getAbsolutePath());

            try {
                boolean success = PDFWatermarkApplier.applyWatermark(
                        output,
                        tempFile,
                        ConversionContextReader.getWatermark()
                );

                logger.info("Watermark application completed, success: {}", success);

                if (success && tempFile.exists() && tempFile.length() > 0) {
                    logger.info("Watermark applied successfully, replacing original file");

                    // Usa Files.move() per sostituzione atomica
                    try {
                        Files.move(tempFile.toPath(), output.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                        logger.info("File watermarkato sostituito correttamente");
                        return output; // Ritorna sempre pdfFile
                    } catch (IOException e) {
                        logger.warn("Impossibile sostituire il file: {}", e.getMessage());
                        throw new WatermarkException("Impossibile sostituire il file con watermark: " + e.getMessage());
                    }
                } else {
                    logger.warn("Watermark application failed - temp file not created or empty");
                    throw new WatermarkException("Watermark non applicato correttamente");
                }
            } catch (Exception e) {
                throw new WatermarkException("Impossibile applicare il watermark: " + e.getMessage());
            }
        } else {
            logger.info("No watermark specified - skipping watermark application");
        }

        return output;
    }
}