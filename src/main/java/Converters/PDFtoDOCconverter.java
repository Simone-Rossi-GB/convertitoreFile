package Converters;

import java.util.ArrayList;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;

import java.io.*;

public class PDFtoDOCconverter implements Converter{

    @Override
    public ArrayList<File> convert(File pdfFile) throws IOException {
        ArrayList<File> files = new ArrayList<>();
        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(baseName + ".doc");
        File thempleate  = new File("convertitoreFile/themplate.doc");
        PDDocument pdf = PDDocument.load(pdfFile);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(pdf);
        pdf.close();

        FileInputStream fis = new FileInputStream(thempleate);
        HWPFDocument doc = new HWPFDocument(fis);
        fis.close();

        Range range = doc.getRange();
        range.replaceText(range.text(), text);

        FileOutputStream out = new FileOutputStream(outputFile);
        doc.write(out);
        out.close();

        files.add(outputFile);
        return files;
    }
}
