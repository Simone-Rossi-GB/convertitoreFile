import Converters.*;
import com.itextpdf.text.DocumentException;

import java.io.IOException;

public class mainProvaMSGEML {
    public static void main(String[] args) throws IOException {
        ConverterConfig config = new ConverterConfig();
        EMLtoPDF converterEMLtoPDF = new EMLtoPDF();
        MSGtoPDF converterMSGtoPDF = new MSGtoPDF();

        try {
            converterEMLtoPDF.convert("src/main/java/Converters/email.eml", "src/main/java/Converters/email_eml.pdf");
            converterMSGtoPDF.convert("src/main/java/Converters/email.msg", "src/main/java/Converters/email_msg.pdf");

        } catch (IOException | DocumentException e) {
            System.err.println("Errore: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
