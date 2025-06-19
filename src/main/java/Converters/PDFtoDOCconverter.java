package Converters;

import java.util.ArrayList;

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
        try {
            ArrayList<File> files = new ArrayList<>();
            String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
            File outputFile = new File(baseName + ".doc");
            File template = new File("themplate.doc");

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            try (FileInputStream fis = new FileInputStream(template)) {
                HWPFDocument doc = new HWPFDocument(fis);
                Range range = doc.getRange();
                range.replaceText(range.text(), text);

                try (FileOutputStream out = new FileOutputStream(outputFile)) {
                    doc.write(out);
                }
            }

            files.add(outputFile);
            return files;
        }catch (Exception e){
            throw new Exception("Errore durante il processo di conversione");
        }
    }
}

