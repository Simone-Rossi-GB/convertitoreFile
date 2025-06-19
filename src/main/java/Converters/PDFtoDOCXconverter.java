package Converters;

import converter.Log;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.*;
import java.util.ArrayList;

public class PDFtoDOCXconverter extends AbstractPDFConverter {

    /**
     * Conversione PDF -> DOCX
     * @param pdfFile File PDF di partenza
     * @param pdfDocument Documento PDF caricato
     * @return ArrayList di file DOCX convertiti
     * @throws Exception in caso di errore durante la conversione
     */
    @Override
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
        // Controlla che il file e il documento siano validi
        validateInputs(pdfFile, pdfDocument);

        ArrayList<File> files = new ArrayList<>();
        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(pdfFile.getParent(), baseName + ".docx");

        try {
            Log.addMessage("Estrazione testo dal file PDF: " + pdfFile.getName());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            // Scrittura del testo estratto in formato DOCX
            try (XWPFDocument docx = new XWPFDocument();
                 FileOutputStream out = new FileOutputStream(outputFile)) {

                Log.addMessage("Creazione documento DOCX: " + outputFile.getName());

                // Suddivide il testo in righe e le scrive come paragrafi nel documento
                String[] lines = text.split("\\r?\\n");
                for (String line : lines) {
                    XWPFParagraph paragraph = docx.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(line);
                }

                docx.write(out);
            }

            files.add(outputFile);
            Log.addMessage("Creazione file .docx completata: " + outputFile.getName());
            return files;

        } catch (IOException e) {
            Log.addMessage("ERRORE: problema I/O durante la conversione - " + e.getMessage());
            throw new IOException("Errore I/O durante la conversione del file " + pdfFile.getName(), e);

        } catch (Exception e) {
            Log.addMessage("ERRORE: eccezione durante la conversione - " + e.getMessage());
            throw new Exception("Errore durante la conversione del file " + pdfFile.getName(), e);
        }
    }
}
