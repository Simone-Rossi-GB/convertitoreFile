package webService.converters;

import converter.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Convertitore da PDF a DOCX.
 * Estrae il testo da un PDF e lo inserisce in un documento Word .docx.
 */
public class PDFtoDOCXconverter extends AbstractPDFConverter {

    private static final Logger logger = LogManager.getLogger(PDFtoDOCXconverter.class);

    /**
     * Conversione PDF -> DOCX
     *
     * @param pdfFile     File PDF di partenza
     * @param pdfDocument Documento PDF caricato
     * @return ArrayList contenente il file DOCX generato
     * @throws Exception in caso di errore durante la conversione
     */
    @Override
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
        validateInputs(pdfFile, pdfDocument);
        logger.info("Inizio conversione con parametri: \n | pdfFile.getPath() = {}", pdfFile.getPath());
        ArrayList<File> files = new ArrayList<>();
        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(pdfFile.getParent(), baseName + ".docx");

        try {
            logger.info("Estrazione testo dal file PDF: {}", pdfFile.getName());
            Log.addMessage("[PDF→DOCX] Estrazione testo da: " + pdfFile.getName());

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            try (XWPFDocument docx = new XWPFDocument();
                 FileOutputStream out = new FileOutputStream(outputFile)) {

                logger.info("Creazione documento DOCX: {}", outputFile.getName());
                Log.addMessage("[PDF→DOCX] Creazione documento DOCX: " + outputFile.getName());

                String[] lines = text.split("\\r?\\n");
                for (String line : lines) {
                    XWPFParagraph paragraph = docx.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(line);
                }

                docx.write(out);
            }

            files.add(outputFile);
            logger.info("Creazione file .docx completata: {}", outputFile.getName());
            Log.addMessage("[PDF→DOCX] Creazione completata: " + outputFile.getName());

            return files;

        } catch (IOException e) {
            logger.error("Errore I/O durante la conversione: {}", e.getMessage());
            Log.addMessage("[PDF→DOCX] ERRORE: problema I/O - " + e.getMessage());
            throw new IOException("Errore I/O durante la conversione del file " + pdfFile.getName(), e);

        } catch (Exception e) {
            logger.error("Eccezione durante la conversione: {}", e.getMessage());
            Log.addMessage("[PDF→DOCX] ERRORE: eccezione durante la conversione - " + e.getMessage());
            throw new Exception("Errore durante la conversione del file " + pdfFile.getName(), e);
        }
    }
}
