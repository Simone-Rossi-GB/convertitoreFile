package Converters;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.*;
import java.util.ArrayList;

public class PDFtoDOCXconverter implements Converter {
    @Override
    public ArrayList<File> convert(File pdfFile) throws IOException {
        ArrayList<File> files = new ArrayList<>();
        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(baseName + ".docx");
        try (PDDocument documento = PDDocument.load(pdfFile);
             XWPFDocument docx = new XWPFDocument();
             FileOutputStream out = new FileOutputStream(outputFile)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(documento);

            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                XWPFParagraph paragrafo = docx.createParagraph();
                XWPFRun run = paragrafo.createRun();
                run.setText(line);
            }
            docx.write(out);
            files.add(outputFile);
        }
        return files;
    }
}
