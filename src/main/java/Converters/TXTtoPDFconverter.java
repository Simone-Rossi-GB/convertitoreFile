package Converters;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.*;
import java.util.ArrayList;

public class TXTtoPDFconverter implements Converter {
    @Override
    public ArrayList<File> convert(File srcFile) throws IOException, DocumentException {
        ArrayList<File> result = new ArrayList<>();
        File output = new File(srcFile.getAbsolutePath().replaceAll("\\.txt$", ".pdf"));

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(output));
        document.open();

        try (BufferedReader reader = new BufferedReader(new FileReader(srcFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                document.add(new Paragraph(line));
            }
        }
        document.close();
        result.add(output);
        return result;
    }
}


