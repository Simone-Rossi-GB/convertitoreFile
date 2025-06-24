package converters.pdfConverters;

import converters.exception.ConversionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import java.net.ConnectException;
import java.util.*;

/**
 * Convertitore da PDF a DOCX.
 * <p>
 * Estrae il testo con riconoscimento delle righe e inserisce le immagini contenute nel PDF all'interno
 * del documento Word generato.
 */
public class PDFtoDOCXconverter extends AbstractPDFConverter {

    private static final Logger logger = LogManager.getLogger(PDFtoDOCXconverter.class);

    /**
     * Metodo principale di conversione.
     *
     * @param pdfFile      il file PDF di input
     * @param pdfDocument  documento PDFBox già caricato
     * @return file DOCX temporaneo generato
     * @throws Exception in caso di errore nella conversione
     */
    @Override
    protected File convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception {
        logger.info("Inizio conversione PDF → DOCX: {}", pdfFile.getName());

        XWPFDocument docx = new XWPFDocument();

        // Estrai solo il testo
        try {
            PDFTextStripper stripper = new LineAwareStripper(docx);
            stripper.setSortByPosition(true);
            stripper.setStartPage(1);
            stripper.setEndPage(pdfDocument.getNumberOfPages());

            stripper.writeText(pdfDocument, new OutputStreamWriter(new ByteArrayOutputStream()));
        } catch (Exception e) {
            logger.error("Errore durante l'estrazione del testo: {}", e.getMessage(), e);
            throw new ConversionException("Errore durante l'estrazione del testo dal PDF");
        }

        // Estrai immagini da ogni pagina
        try {
            for (int i = 0; i < pdfDocument.getNumberOfPages(); i++) {
                PDPage page = pdfDocument.getPage(i);
                PDResources resources = page.getResources();
                if (resources == null) continue;

                for (COSName xObjectName : resources.getXObjectNames()) {
                    try {
                        PDXObject xObject = resources.getXObject(xObjectName);
                        if (xObject instanceof PDImageXObject) {
                            PDImageXObject image = (PDImageXObject) xObject;
                            BufferedImage bImage = image.getImage();
                            insertImageInDocx(docx, bImage);
                            logger.info("Immagine inserita dalla pagina {}", i + 1);
                        }
                    } catch (Exception e) {
                        logger.warn("Errore nel recuperare un'immagine dalla pagina {}: {}", i + 1, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Errore durante l'estrazione delle immagini: {}", e.getMessage(), e);
            throw new ConversionException("Errore durante l'estrazione delle immagini dal PDF");
        }

        File outputFile = new File(getTempFileName(pdfFile));
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            docx.write(out);
        } catch (Exception e) {
            logger.error("Errore nella scrittura del file DOCX: {}", e.getMessage(), e);
            throw new ConversionException("Errore nella scrittura del file DOCX");
        }

        logger.info("Conversione completata: {}", outputFile.getAbsolutePath());
        return outputFile;
    }

    /**
     * Genera un nome di file temporaneo per il file DOCX.
     *
     * @param inputFile file di partenza (PDF)
     * @return percorso del file temporaneo DOCX
     */
    protected String getTempFileName(File inputFile) {
        String baseName = inputFile.getName();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) baseName = baseName.substring(0, dotIndex);
        return System.getProperty("java.io.tmpdir") + File.separator + baseName + ".docx";
    }

    /**
     * Inserisce un'immagine ridimensionata nel documento DOCX.
     *
     * @param docx  documento Word
     * @param image immagine da inserire
     * @throws IOException in caso di errore di scrittura
     */
    private void insertImageInDocx(XWPFDocument docx, BufferedImage image) throws IOException {
        final int maxWidthPx = 500;

        int width = image.getWidth();
        int height = image.getHeight();

        // Scala immagine mantenendo proporzioni
        if (width > maxWidthPx) {
            float scale = (float) maxWidthPx / width;
            width = maxWidthPx;
            height = (int) (height * scale);
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             InputStream is = new ByteArrayInputStream(os.toByteArray())) {
            ImageIO.write(image, "png", os);

            XWPFParagraph paragraph = docx.createParagraph();
            XWPFRun run = paragraph.createRun();

            run.addPicture(is,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "image.png",
                    Units.toEMU(width),
                    Units.toEMU(height));
        } catch (Exception e) {
            logger.error("Inserimento immagine fallito: {}", e.getMessage(), e);
            throw new ConnectException("Errore durante l'inserimento immagine nel DOCX");
        }
    }

    /**
     * Estensione di PDFTextStripper per rilevare i cambi di linea
     * e replicare il layout verticale del testo nel documento DOCX.
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
        protected void writeString(String text, List<TextPosition> positions) {
            if (positions.isEmpty()) return;

            float currentY = positions.get(0).getYDirAdj();

            try {
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
            } catch (Exception e) {
                logger.warn("Errore durante la scrittura del testo: {}", e.getMessage());
            }
        }

        @Override
        protected void endPage(PDPage page) {
            lastY = -1;
            currentParagraph = null;
        }
    }
}
