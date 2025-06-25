package webService.server.converters.pdfConverters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Convertitore da PDF a DOC (.doc classico).
 */
public class PDFtoDOCconverter extends AbstractPDFConverter {

    private static final Logger logger = LogManager.getLogger(PDFtoDOCconverter.class);

    @Override
    protected File convertInternal(File pdfFile, PDDocument pdfDocument) throws IOException {
        logger.info("Inizio conversione PDF → DOC: {}", pdfFile.getName());

        // Estrai testo dal PDF
        PDFTextStripper stripper = new PDFTextStripper();
        String extractedText = stripper.getText(pdfDocument);

        // Crea un template DOC vuoto (se non esiste già, crea da zero)
        File outputFile = new File(getTempFileName(pdfFile));
        HWPFDocument doc ;

        try (InputStream templateStream = Files.newInputStream(Paths.get("themplate.doc"))) {
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
