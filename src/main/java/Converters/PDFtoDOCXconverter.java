package Converters;

import converter.Log;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.*;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class PDFtoDOCXconverter extends AbstractPDFConverter {

    protected File convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
        XWPFDocument docx = new XWPFDocument();

        // Imposta larghezza pagina come prima (omesso per brevità)

        PDFTextStripper stripper = new LineAwareStripper(docx);
        stripper.setSortByPosition(true);
        stripper.setStartPage(1);
        stripper.setEndPage(pdfDocument.getNumberOfPages());

        // Estrazione testo (come prima)
        stripper.writeText(pdfDocument, new OutputStreamWriter(new ByteArrayOutputStream()));

        // **Qui inseriamo il codice per estrarre immagini per pagina**
        for (int i = 0; i < pdfDocument.getNumberOfPages(); i++) {
            PDPage page = pdfDocument.getPage(i);
            PDResources resources = page.getResources();

            if (resources == null) continue;

            Iterable<COSName> xObjectNames = resources.getXObjectNames();
            for (COSName xObjectName : xObjectNames) {
                PDXObject xObject = resources.getXObject(xObjectName);
                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;
                    BufferedImage bImage = image.getImage();

                    // Inserisci immagine nel docx
                    insertImageInDocx(docx, bImage);
                }
            }
        }

        // Salvataggio file (come prima)
        File outputFile = new File(getTempFileName(pdfFile));
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            docx.write(out);
        }

        return outputFile;
    }


    protected String getTempFileName(File inputFile) {
        String baseName = inputFile.getName();
        int dotIndex = baseName.lastIndexOf('.');
        return System.getProperty("java.io.tmpdir") + File.separator + baseName + ".docx";
    }

    /**
     * Strip del testo con riconoscimento automatico degli a capo reali (via coordinate verticali)
     */
    private static class LineAwareStripper extends PDFTextStripper {
        private final XWPFDocument docx;
        private float lastY = -1;
        private XWPFParagraph currentParagraph;
        private XWPFRun currentRun;


        public LineAwareStripper(XWPFDocument docx) throws IOException {
            this.docx = docx;
        }

        @Override
        protected void writeString(String text, List<TextPosition> positions) throws IOException {
            if (positions.isEmpty()) return;

            float currentY = positions.get(0).getYDirAdj();

            if (currentParagraph == null || Math.abs(currentY - lastY) > 2.5f) {
                currentParagraph = docx.createParagraph();
                currentRun = currentParagraph.createRun();

                TextPosition first = positions.get(0);
                String rawFontName = first.getFont().getName();
                String fontName = rawFontName.toLowerCase();

                // Patch: evita font Symbol/ZapfDingbats
                if (fontName.contains("symbol") || fontName.contains("zapfdingbats")) {
                    currentRun.setFontFamily("Calibri");
                } else {
                    currentRun.setFontFamily(rawFontName);
                }

                currentRun.setFontSize((int) first.getFontSizeInPt());
                if (fontName.contains("bold")) currentRun.setBold(true);
                if (fontName.contains("italic") || fontName.contains("oblique")) currentRun.setItalic(true);
            }

            currentRun.setText(text, currentRun.getTextPosition() == 0 ? 0 : currentRun.getTextPosition());
            lastY = currentY;
        }

        @Override
        protected void endPage(PDPage page) {
            lastY = -1;
            currentParagraph = null;
        }
    }

    private void insertImageInDocx(XWPFDocument docx, BufferedImage image) throws IOException {
        // Definisci larghezza massima desiderata in pixel
        final int maxWidthPx = 500;

        int width = image.getWidth();
        int height = image.getHeight();

        // Calcola scala mantenendo proporzioni
        if (width > maxWidthPx) {
            float scale = (float) maxWidthPx / width;
            width = maxWidthPx;
            height = (int) (height * scale);
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(image, "png", os);
        InputStream is = new ByteArrayInputStream(os.toByteArray());

        XWPFParagraph paragraph = docx.createParagraph();
        XWPFRun run = paragraph.createRun();

        try {
            run.addPicture(is,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "image.png",
                    Units.toEMU(width),
                    Units.toEMU(height));
        } catch (Exception e) {
            Log.addMessage("ERRORE: inserimento immagine fallito - " + e.getMessage());
        } finally {
            is.close();
        }
    }

}