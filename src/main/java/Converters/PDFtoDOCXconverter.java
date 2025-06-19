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
     * Conversione PDF -> DOCX
     * @param pdfFile File PDF di partenza
     * @param pdfDocument Documento PDF caricato
     * @return ArrayList di file DOCX convertiti
     * @throws Exception in caso di errore durante la conversione
     */
    @Override
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
        validateInputs(pdfFile, pdfDocument);

        ArrayList<File> files = new ArrayList<>();
        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(pdfFile.getParent(), baseName + ".docx");

        try {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);

            try (XWPFDocument docx = new XWPFDocument();
                 FileOutputStream out = new FileOutputStream(outputFile)) {

                String[] lines = text.split("\\r?\\n");
                for (String line : lines) {
                    XWPFParagraph paragraph = docx.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(line);
                }

                docx.write(out);
            }

            files.add(outputFile);
            return files;

        } catch (Exception e) {
            throw new Exception("Errore durante la conversione: " + e.getMessage(), e);
        }
    }
}
