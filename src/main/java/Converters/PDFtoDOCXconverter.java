package Converters;

import converter.Log;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

import javax.imageio.ImageIO;

public class PDFtoDOCXconverter extends AbstractPDFConverter {

    @Override
    protected File convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
        XWPFDocument docx = new XWPFDocument();

        try {
            // 1. Estrai il testo con formattazione base
            PDFTextStripper stripper = new PDFTextStripper() {

                @Override
                protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
                    XWPFParagraph para = docx.createParagraph();
                    XWPFRun run = para.createRun();

                    for (TextPosition text : textPositions) {
                        String c = text.getUnicode();
                        run = para.createRun();
                        run.setText(c);

                        run.setFontSize((int) text.getFontSizeInPt());

                        String fontName = text.getFont().getName().toLowerCase();
                        run.setFontFamily(text.getFont().getName());

                        if (fontName.contains("bold")) run.setBold(true);
                        if (fontName.contains("italic") || fontName.contains("oblique")) run.setItalic(true);
                    }

                    docx.createParagraph(); // a capo dopo ogni blocco
                }
            };

            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(pdfDocument.getNumberOfPages());
            stripper.writeText(pdfDocument, null);

            // 2. Estrai e inserisci immagini convertite
            for (PDPage page : pdfDocument.getPages()) {
                PDResources resources = page.getResources();
                for (org.apache.pdfbox.cos.COSName name : resources.getXObjectNames()) {
                    if (resources.isImageXObject(name)) {
                        PDImageXObject image = (PDImageXObject) resources.getXObject(name);
                        BufferedImage bimg = image.getImage();

                        File tempImage = File.createTempFile("pdfimg_", ".png");
                        ImageIO.write(bimg, "png", tempImage);

                        ImageConverter imageConverter = new ImageConverter();
                        File convertedImage = imageConverter.convert(tempImage); // usa il tuo metodo

                        try (InputStream imgInput = Files.newInputStream(convertedImage.toPath())) {
                            XWPFParagraph imgPara = docx.createParagraph();
                            XWPFRun imgRun = imgPara.createRun();

                            imgRun.addPicture(imgInput,
                                    XWPFDocument.PICTURE_TYPE_PNG,
                                    convertedImage.getName(),
                                    Units.toEMU(300),
                                    Units.toEMU(200));
                        } catch (Exception e) {
                            Log.addMessage("ERRORE: inserimento immagine fallito - " + e.getMessage());
                        }

                        tempImage.delete();
                        convertedImage.delete();
                    }
                }
            }

            // 3. Salva il file DOCX finale
            File outputFile = new File(getTempFileName(pdfFile));
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                docx.write(out);
            }

            return outputFile;

        } catch (Exception e) {
            Log.addMessage("ERRORE: conversione PDF->DOCX fallita - " + e.getMessage());
            throw e;
        } finally {
            pdfDocument.close();
            docx.close();
        }
    }

    protected String getTempFileName(File inputFile) {
        String baseName = inputFile.getName();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = baseName.substring(0, dotIndex);
        }
        String unique = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 8);
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        return new File(tempDir, baseName + "_" + unique + ".docx").getAbsolutePath();
    }
}

