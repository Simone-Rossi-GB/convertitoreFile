package webService.server.converters.txtConverters;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.*;
import java.nio.file.Files;

import webService.server.converters.Converter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class TXTtoPDFconverter extends Converter {
    private static final Logger logger = LogManager.getLogger(TXTtoPDFconverter.class);

    /**
     * Conversione txt -> docx
     * @param srcFile file di partenza
     * @return file convertito
     * @throws IOException errori di lettura/scrittura sul file
     */
    @Override
    public File convert(File srcFile) throws IOException, DocumentException {
        logger.info("Conversione iniziata con parametri:\n | srcFile.getPath() = {}", srcFile.getPath());
        File output = new File(srcFile.getAbsolutePath().replaceAll("\\.txt$", ".pdf"));
        //Crea un nuovo documento pdf
        Document document = new Document();
        PdfWriter.getInstance(document, Files.newOutputStream(output.toPath()));
        document.open();

        try (BufferedReader reader = new BufferedReader(new FileReader(srcFile))) {
            String line;
            //Per ogni riga aggiunge al pdf un paragrafo
            while ((line = reader.readLine()) != null) {
                document.add(new Paragraph(line));
            }
        }
        document.close();
        return output;
    }
}


