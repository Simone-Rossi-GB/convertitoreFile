package Converters;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class TXTtoPDFconverter implements Converter {
    private static final Logger logger = LogManager.getLogger(TXTtoPDFconverter.class);
    @Override
    public ArrayList<File> convert(File srcFile) throws IOException, DocumentException {
        logger.info("Conversione iniziata con parametri:\n | srcFile.getPath() = {}", srcFile.getPath());
        ArrayList<File> result = new ArrayList<>();
        File output = new File(srcFile.getAbsolutePath().replaceAll("\\.txt$", ".pdf"));

        Document document = new Document();
        PdfWriter.getInstance(document, Files.newOutputStream(output.toPath()));
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


