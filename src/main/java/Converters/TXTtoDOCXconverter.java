package Converters;

import java.io.*;
import java.util.ArrayList;
import org.apache.poi.xwpf.usermodel.*;

public class TXTtoDOCXconverter implements Converter {
    @Override
    public ArrayList<File> convert(File srcFile) throws IOException {
        ArrayList<File> result = new ArrayList<>();
        File output = new File(srcFile.getAbsolutePath().replaceAll("\\.txt$", ".docx"));

        try (XWPFDocument doc = new XWPFDocument();
             BufferedReader reader = new BufferedReader(new FileReader(srcFile));
             FileOutputStream out = new FileOutputStream(output)) {

            String line;
            while ((line = reader.readLine()) != null) {
                XWPFParagraph paragraph = doc.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line);
            }
            doc.write(out);
        }

        result.add(output);
        return result;
    }
}
