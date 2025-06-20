package Converters;

import java.util.ArrayList;

import converter.Log;
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
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
        // Validazione degli input
        logger.info("Inizio conversione con parametri: \n | pdfFile.getPath() = {}", pdfFile.getPath());
        validateInputs(pdfFile, pdfDocument);

        ArrayList<File> files = new ArrayList<>();
        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(baseName + ".doc");
        File template = new File("themplate.doc");

        try {
            if (!template.exists()) {
                logger.error("Template mancante: {}", template.getAbsolutePath());
                Log.addMessage("[PDF→DOC] ERRORE: file template 'themplate.doc' non trovato.");
                throw new FileNotFoundException("Template mancante");
            }

            Log.addMessage("[PDF→DOC] Estrazione testo dal file: " + pdfFile.getName());
            logger.info("Estrazione testo da PDF: {}", pdfFile.getName());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            try (FileInputStream fis = new FileInputStream(template);
                 HWPFDocument doc = new HWPFDocument(fis);
                 FileOutputStream out = new FileOutputStream(outputFile)) {

                logger.info("Scrittura contenuto nel file DOC: {}", outputFile.getName());
                Log.addMessage("[PDF→DOC] Scrittura nel file DOC: " + outputFile.getName());

                Range range = doc.getRange();
                range.replaceText(range.text(), text);
                doc.write(out);

                files.add(outputFile);

                logger.info("File .doc creato con successo: {}", outputFile.getAbsolutePath());
                Log.addMessage("[PDF→DOC] Creazione completata: " + outputFile.getName());
                return files;
            }

        } catch (FileNotFoundException e) {
            logger.error("File non trovato: {}", e.getMessage());
            Log.addMessage("[PDF→DOC] ERRORE: file non trovato - " + e.getMessage());
            throw e;

        } catch (IOException e) {
            logger.error("Errore I/O durante la conversione: {}", e.getMessage());
            Log.addMessage("[PDF→DOC] ERRORE: problema I/O - " + e.getMessage());
            throw new IOException("Errore I/O durante la conversione del file " + pdfFile.getName(), e);

        } catch (Exception e) {
            logger.error("Errore generico nella conversione: {}", e.getMessage());
            Log.addMessage("[PDF→DOC] ERRORE: eccezione durante la conversione - " + e.getMessage());
            throw new Exception("Errore generico durante la conversione del file " + pdfFile.getName(), e);
        }
    }
}
