package webService.server.converters;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import webService.server.converters.txtConverters.TXTtoPDFconverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class PdfPasswordApplier {
    private static final Logger logger = LogManager.getLogger(PdfPasswordApplier.class);
    public static void encryptPDF(File inputPDF, String password) throws IOException, DocumentException {
        File encryptedPDF = new File(inputPDF.getParent(), "encrypted_" + inputPDF.getName());

        PdfReader reader = new PdfReader(inputPDF.getAbsolutePath());
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(encryptedPDF));

        stamper.setEncryption(
                password.getBytes(),
                password.getBytes(),
                PdfWriter.ALLOW_PRINTING,
                PdfWriter.ENCRYPTION_AES_128
        );

        stamper.close();
        reader.close();

        // Sovrascrive l'originale
        Files.move(encryptedPDF.toPath(), inputPDF.toPath(), StandardCopyOption.REPLACE_EXISTING);
        logger.info("Password applicata correttamente al documento");
    }
}
