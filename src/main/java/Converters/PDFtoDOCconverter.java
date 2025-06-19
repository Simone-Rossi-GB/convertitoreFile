package Converters;

import java.util.ArrayList;

import converter.Log;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessBufferedFileInputStream;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;

import java.io.*;

public class PDFtoDOCconverter extends AbstractPDFConverter {
    /**
     * Conversione pdf -> doc
     * @param pdfFile File di partenza
     * @param pdfDocument Documento pdf caricato
     * @return @return ArrayList di file convertiti
     * @throws Exception Errore durante il processo di conversione
     */
    @Override
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
        // Validazione degli input
        validateInputs(pdfFile, pdfDocument);

        ArrayList<File> files = new ArrayList<>();
        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(baseName + ".doc");
        File template = new File("themplate.doc");

        try {
            // Controlla l'esistenza del template
            if (!template.exists()) {
                Log.addMessage("ERRORE: file template 'themplate.doc' non trovato");
                throw new FileNotFoundException("Template mancante");
            }

            Log.addMessage("Estrazione testo dal file PDF: " + pdfFile.getName());
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            // Apertura del template Word e scrittura del testo estratto
            try (FileInputStream fis = new FileInputStream(template);
                 HWPFDocument doc = new HWPFDocument(fis);
                 FileOutputStream out = new FileOutputStream(outputFile)) {

                Log.addMessage("Scrittura testo nel file DOC: " + outputFile.getName());
                Range range = doc.getRange();
                range.replaceText(range.text(), text);
                doc.write(out);

                files.add(outputFile);
                Log.addMessage("Creazione file .doc completata: " + outputFile.getName());
                return files;

            }
        } catch (FileNotFoundException e) {
            // Template o PDF mancante
            Log.addMessage("ERRORE: file non trovato - " + e.getMessage());
            throw e;

        } catch (IOException e) {
            Log.addMessage("ERRORE: problema I/O durante la conversione - " + e.getMessage());
            throw new IOException("Errore I/O durante la conversione del file " + pdfFile.getName(), e);

        } catch (Exception e) {
            Log.addMessage("ERRORE: eccezione durante la conversione - " + e.getMessage());
            throw new Exception("Errore generico durante la conversione del file " + pdfFile.getName(), e);
        }
    }



}

