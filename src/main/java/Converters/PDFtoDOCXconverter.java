package Converters;

import converter.Log;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PDFtoDOCXconverter extends AbstractPDFConverter {

    protected File convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
        XWPFDocument docx = new XWPFDocument();

        LineAwareStripper stripper = new LineAwareStripper(docx);
        stripper.setSortByPosition(true);
        stripper.setStartPage(1);
        stripper.setEndPage(pdfDocument.getNumberOfPages());

        // Estrai il testo
        stripper.writeText(pdfDocument, new OutputStreamWriter(new ByteArrayOutputStream()));

        // Se ci sono righe potenzialmente tabellari, prova a inserirle come tabella
        if (!stripper.extractedLines.isEmpty()) {
            insertTableFromLines(docx, stripper.extractedLines);
        }

        // Estrazione immagini
        for (int i = 0; i < pdfDocument.getNumberOfPages(); i++) {
            PDPage page = pdfDocument.getPage(i);
            PDResources resources = page.getResources();
            if (resources == null) continue;

            for (COSName xObjectName : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(xObjectName);
                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;
                    BufferedImage bImage = image.getImage();
                    insertImageInDocx(docx, bImage);
                }
            }
        }

        // Salvataggio
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

    private static class LineAwareStripper extends PDFTextStripper {
        private final XWPFDocument docx;
        private float lastY = -1;
        private XWPFParagraph currentParagraph;
        private XWPFRun currentRun;
        public List<String> extractedLines = new ArrayList<>();

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

            // Salva la riga anche in plain text per uso successivo
            extractedLines.add(text.trim());
        }

        @Override
        protected void endPage(PDPage page) {
            lastY = -1;
            currentParagraph = null;
        }
    }

    private void insertTableFromLines(XWPFDocument docx, List<String> lines) {
        List<String[]> tableRows = new ArrayList<>();

        for (String line : lines) {
            if (line.matches(".*(\\t|\\s{2,}).*")) {
                String[] cells = line.split("\\t|\\s{2,}"); // tab o almeno due spazi
                tableRows.add(cells);
            }
        }

        if (tableRows.isEmpty()) return;

        int columnCount = Arrays.stream(tableRows.toArray(new String[0][]))
                .mapToInt(a -> a.length)
                .max().orElse(1);

        XWPFTable table = docx.createTable(tableRows.size(), columnCount);

        for (int i = 0; i < tableRows.size(); i++) {
            String[] cells = tableRows.get(i);
            for (int j = 0; j < cells.length; j++) {
                table.getRow(i).getCell(j).setText(cells[j].trim());
            }
        }

        // Aggiungi una riga vuota dopo la tabella per separazione
        docx.createParagraph();
    }

    private void insertImageInDocx(XWPFDocument docx, BufferedImage image) throws IOException {
        int maxWidthPx = 500;
        int width = image.getWidth();
        int height = image.getHeight();

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
            run.addPicture(is, XWPFDocument.PICTURE_TYPE_PNG, "image.png",
                    Units.toEMU(width), Units.toEMU(height));
        } catch (Exception e) {
            Log.addMessage("ERRORE: inserimento immagine fallito - " + e.getMessage());
        } finally {
            is.close();
        }
    }
}
