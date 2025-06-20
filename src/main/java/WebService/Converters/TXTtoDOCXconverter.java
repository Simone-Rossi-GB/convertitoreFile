package WebService.Converters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.*;
import java.util.ArrayList;


public class TXTtoDOCXconverter implements Converter {
    private static final Logger logger = LogManager.getLogger(ZIPtoTARGZconverter.class);
    @Override
    public ArrayList<File> convert(File srcFile) throws IOException {
        logger.info("Conversione iniziata con parametri:\n | srcFile.getPath() = {}", srcFile.getPath());
        ArrayList<File> result = new ArrayList<>();
        File output = new File(srcFile.getAbsolutePath().replaceAll("\\.txt$", ".docx"));

        XWPFDocument doc = new XWPFDocument();
        try (BufferedReader reader = new BufferedReader(new FileReader(srcFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                XWPFParagraph paragraph = doc.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line);
            }
        }

        try (FileOutputStream out = new FileOutputStream(output)) {
            doc.write(out);
        }

        result.add(output);
        return result;
    }
}