package WebService.Converters;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;


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


