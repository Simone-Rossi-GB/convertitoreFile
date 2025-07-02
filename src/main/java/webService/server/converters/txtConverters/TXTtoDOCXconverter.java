package webService.server.converters.txtConverters;

import java.io.*;

import webService.server.converters.Converter;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class TXTtoDOCXconverter extends Converter {
    private static final Logger logger = LogManager.getLogger(TXTtoDOCXconverter.class);

    /**
     * Conversione txt -> docx
     * @param srcFile file di partenza
     * @return file convertito
     * @throws IOException errori di lettura/scrittura sul file
     */
    @Override
    public File convert(File srcFile) throws IOException {
        logger.info("Conversione iniziata con parametri:\n | srcFile.getPath() = {}", srcFile.getPath());
        File output = new File(srcFile.getAbsolutePath().replaceAll("\\.txt$", ".docx"));
        //Crea un documento word vuoto
        try (XWPFDocument doc = new XWPFDocument()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(srcFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    //Crea un paragrafo per ogni riga
                    XWPFParagraph paragraph = doc.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(line);
                }
            }

            try (FileOutputStream out = new FileOutputStream(output)) {
                //Scrive il contenuto nel file .docx
                doc.write(out);
            }
            return output;
        }
    }
}