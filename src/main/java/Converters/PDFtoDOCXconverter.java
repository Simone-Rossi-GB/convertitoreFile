package Converters;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.*;
import java.util.ArrayList;

public class PDFtoDOCXconverter extends AbstractPDFConverter {

    /**
     * Conversione pdf -> docx
     * @param pdfFile File di partenza
     * @param pdfDocument Documento pdf caricato
     * @return @return ArrayList di file convertiti
     * @throws Exception Errore durante il processo di conversione
     */
    @Override
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception{
        try {
            ArrayList<File> files = new ArrayList<>();
            String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
            File outputFile = new File(baseName + ".docx");

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            try (XWPFDocument docx = new XWPFDocument();
                 FileOutputStream out = new FileOutputStream(outputFile)) {

                String[] lines = text.split("\\r?\\n");
                for (String line : lines) {
                    XWPFParagraph par = docx.createParagraph();
                    XWPFRun run = par.createRun();
                    run.setText(line);
                }

                docx.write(out);
            }

            files.add(outputFile);
            return files;
        }catch (Exception e){
            throw new Exception("Errore durante la conversione");
        }
    }
}
