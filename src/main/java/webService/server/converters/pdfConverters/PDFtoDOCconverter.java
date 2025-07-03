package webService.server.converters.pdfConverters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import webService.server.config.configHandlers.Config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Convertitore da PDF a DOC (.doc classico).
 */
public class PDFtoDOCconverter extends AbstractPDFConverter {

    private static final Logger logger = LogManager.getLogger(PDFtoDOCconverter.class);

    @Override
    protected File convertInternal(File pdfFile, PDDocument pdfDocument, Config configuration) throws IOException {
        logger.info("Inizio conversione PDF → DOC: {}", pdfFile.getName());

        // Estrai testo dal PDF
        PDFTextStripper stripper = new PDFTextStripper();
        String extractedText = stripper.getText(pdfDocument);

        // Crea un template DOC vuoto (se non esiste già, crea da zero)
        File outputFile = new File(getTempFileName(pdfFile));
        HWPFDocument doc;

        // CORREZIONE: Usa ClassLoader per accedere al file in resources
        try (InputStream templateStream = getClass().getClassLoader().getResourceAsStream("themplate.doc")) {
            if (templateStream == null) {
                logger.error("Template 'themplate.doc' non trovato nella cartella resources");
                throw new IOException("Template themplate.doc non trovato nella cartella resources");
            }

            logger.info("Template trovato, caricamento in corso...");
            doc = new HWPFDocument(templateStream);
        }

        // Inserisci il testo nel documento .doc
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            Range range = doc.getRange();

            // Evita il delete, usa replaceText in modo sicuro
            String existingText = range.text();
            if (existingText != null && !existingText.isEmpty()) {
                range.replaceText(existingText, extractedText);
            } else {
                range.insertAfter(extractedText);
            }
            doc.write(out);
        }

        logger.info("File DOC generato: {}", outputFile.getAbsolutePath());
        return outputFile;
    }

    private String getTempFileName(File inputFile) {
        String baseName = inputFile.getName().replaceAll("(?i)\\.pdf$", "");
        return System.getProperty("java.io.tmpdir") + File.separator + baseName + ".doc";
    }
}