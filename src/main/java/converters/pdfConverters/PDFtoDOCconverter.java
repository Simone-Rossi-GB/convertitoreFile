package converters.pdfConverters;

import converters.exception.ConversionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;

/**
 * Convertitore da PDF a DOC.
 * Estrae il testo da un file PDF e lo inserisce in un file Word .doc usando un template esistente.
 */
public class PDFtoDOCconverter extends AbstractPDFConverter {

    private static final Logger logger = LogManager.getLogger(PDFtoDOCconverter.class);

    /**
     * Conversione pdf -> doc
     *
     * @param pdfFile      File di partenza
     * @param pdfDocument  Documento PDF caricato
     * @return ArrayList contenente il file .doc generato
     * @throws Exception Errore durante il processo di conversione
     */
    @Override
    protected File convertInternal(File pdfFile, PDDocument pdfDocument) throws IOException {
        // Validazione degli input
        logger.info("Inizio conversione con parametri: \n | pdfFile.getPath() = {}", pdfFile.getPath());
        validateInputs(pdfFile, pdfDocument);

        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(baseName + ".doc");
        File template = new File("themplate.doc");

        try {
            if (!template.exists()) {
                logger.error("Template mancante: {}", template.getAbsolutePath());
                throw new FileNotFoundException("Template mancante");
            }

            logger.info("Estrazione testo da PDF: {}", pdfFile.getName());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            try (FileInputStream fis = new FileInputStream(template);
                 HWPFDocument doc = new HWPFDocument(fis);
                 FileOutputStream out = new FileOutputStream(outputFile)) {

                logger.info("Scrittura contenuto nel file DOC: {}", outputFile.getName());

                Range range = doc.getRange();
                range.replaceText(range.text(), text);
                doc.write(out);


                logger.info("File .doc creato con successo: {}", outputFile.getAbsolutePath());
                return outputFile;
            }

        } catch (FileNotFoundException e) {
            logger.error("File non trovato: {}", e.getMessage());
            throw new FileNotFoundException("File non trovato: " + pdfFile.getName());

        } catch (IOException e) {
            logger.error("Errore I/O durante la conversione: {}", e.getMessage());
            throw new ConversionException("Errore I/O durante la conversione del file " + pdfFile.getName());
        }
    }
}
