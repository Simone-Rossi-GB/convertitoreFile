package Converters;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.util.ArrayList;

public class AbstractPDFConverter implements Converter{
    @Override
    public ArrayList<File> convert(File pdfFile) throws Exception {
        return convertWithPassword(pdfFile, null, null);
    }

    @Override
    public ArrayList<File> convert(File pdfFile, boolean union) throws Exception {
        return convertWithPassword(pdfFile, null, union);
    }


    @Override
    public ArrayList<File> convert(File pdfFile, String password) throws Exception {
        return convertWithPassword(pdfFile, password, null);
    }
    @Override
    public ArrayList<File> convert(File pdfFile, String password, boolean union) throws Exception {
        return convertWithPassword(pdfFile, password, union);
    }

    private ArrayList<File> convertWithPassword(File pdfFile, String password, Boolean union) throws Exception {
        if (pdfFile == null) {
            throw new IllegalArgumentException("Il file PDF non può essere nullo");
        }
        PDDocument pdf = null;
        try {
            if (password == null) {
                pdf = PDDocument.load(pdfFile);
            } else {
                pdf = PDDocument.load(pdfFile, password);
            }
            if(union == null)
                return convertInternal(pdfFile, pdf);
            else
                return convertInternal(pdfFile, pdf, union);
        } catch (Exception e) {
            if (password == null) {
                throw new Exception("File protetto da password");
            } else {
                throw new Exception("Password errata");
            }
        } finally {
            if (pdf != null) {
                pdf.close();
            }
        }
    }

    // Metodo astratto da implementare nelle sottoclassi
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception{return null;}

    // Metodo astratto da implementare nelle sottoclassi
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument, boolean union) throws Exception{return null;}


}
