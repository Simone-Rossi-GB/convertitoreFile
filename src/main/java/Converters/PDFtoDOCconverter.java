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

   /* @Override
    public ArrayList<File> convert(File pdfFile) throws Exception {
        System.out.println("Converter");
        ArrayList<File> files = new ArrayList<>();
        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(baseName + ".doc");
        File thempleate  = new File("themplate.doc");

        PDDocument pdf = null;
        try {
            pdf = PDDocument.load(pdfFile);
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
            System.out.println(files);
            return files;
        }catch(Exception e){
            throw new Exception("File protetto da password");
        } finally{
        if (pdf != null)
            pdf.close();
        }
    }

    @Override
    public ArrayList<File> convert(File pdfFile, String password) throws Exception {
        ArrayList<File> files = new ArrayList<>();
        String baseName = pdfFile.getName().replaceAll("(?i)\\.pdf$", "");
        File outputFile = new File(baseName + ".doc");
        File thempleate  = new File("themplate.doc");

        PDDocument pdf = null;
        try {
            pdf = PDDocument.load(pdfFile, password);
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
            System.out.println(files);
            return files;
        }catch(Exception e){
            throw new Exception("Password errata");
        } finally{
            if (pdf != null)
                pdf.close();
        }
    }*/

    @Override
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
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
    }
}

