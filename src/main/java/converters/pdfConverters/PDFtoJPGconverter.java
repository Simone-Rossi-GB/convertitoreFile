package converters.pdfConverters;

import configuration.configHandlers.conversionContext.ConversionContextReader;
import converters.Zipper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class PDFtoJPGconverter extends AbstractPDFConverter {

    private static final Logger logger = LogManager.getLogger(PDFtoJPGconverter.class);

    private static final int DPI = 300; // DPI dell'immagine renderizzata
    private static final int MAX_PAGES = 50; // numero massimo di pagine

    /**
     * Metodo per unire le pagine del pdf una sotto l'altra in un'unica immagine
     *
     * @param images Immagini relative alle singole pagine del pdf
     * @return BufferedImage con tutte le pagine del pdf
     * @throws IllegalArgumentException Lista di immagini null o vuota
     */
    private BufferedImage mergeImagesVertically(ArrayList<BufferedImage> images) throws IllegalArgumentException {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("L'oggetto images non esiste o è vuoto");
        }

        int width = 0;
        int totalHeight = 0;

        //Incrementa, immagine per immagine, l'height di quela finale e seleziona la largherzza massima
        for (BufferedImage img : images) {
            if (img == null) continue; // ignora eventuali immagini null
            width = Math.max(width, img.getWidth());
            totalHeight += img.getHeight();
        }

        BufferedImage combined = new BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = combined.getGraphics();

        int y = 0;
        //Unisce le immagini una sotto l'altra
        for (BufferedImage img : images) {
            if (img == null) continue; // ignora eventuali immagini null
            g.drawImage(img, 0, y, null);
            y += img.getHeight();
        }
        //Rilascia le risorse
        g.dispose();
        logger.info("Immagini unite");
        return combined;
    }

    /**
     * Conversione pdf -> jpg
     *
     * @param pdfFile     File di partenza
     * @param pdfDocument Documento pdf caricato
     * @return immagine con tutte le pagine del pdf o file.zip con un'immagine per pagina
     *
     */
    @Override
    public File convertInternal(File pdfFile, PDDocument pdfDocument) throws IOException {
        //Ottiene il boolean di unione dal JSON
        boolean union = ConversionContextReader.getIsUnion();
        validateInputs(pdfFile, pdfDocument);

        logger.info("Inizio conversione con parametri: \n | pdfFile.getPath() = {}, union={}", pdfFile.getPath(), union);
        int nPages = pdfDocument.getNumberOfPages();

        try {
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            ArrayList<BufferedImage> images = new ArrayList<>();
            ArrayList<File> pages = new ArrayList<>();
            File outputFile = null;

            String baseName = Objects.requireNonNull(pdfFile.getName().replaceAll("(?i)\\.pdf$", ""));
            //Crea una lista di immagini, una per pagina
            for (int i = 0; i < nPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI);
                images.add(image);
                //Se non devono essere unite crea un file per ciascuna
                if (!union) {
                    File tempFile = new File(baseName + "_page_" + (i + 1) + ".jpg");
                    ImageIO.write(image, "jpg", tempFile);
                    pages.add(tempFile);
                }
            }
            //Se devono essere unite chiama il metodo apposito
            if (union) {
                validatePages(nPages);
                BufferedImage mergedImage = mergeImagesVertically(images);
                File mergedFile = new File(baseName + ".jpg");  // usa il nome del PDF
                ImageIO.write(mergedImage, "jpg", mergedFile);
                outputFile = mergedFile;
            }
            //Altrimenti zippa le singole immagini
            else{
                outputFile = Zipper.compressioneFile(pages, baseName);
            }

            pdfDocument.close();
            return outputFile;
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new IOException(e.getMessage());
        }
        catch (IllegalArgumentException e) {
            logger.error(e.getMessage());
            throw new IllegalArgumentException(e.getMessage());
        }finally {
            pdfDocument.close();
        }
    }

    /**
     * Controlla se il numero di pagine del documento rientra nel limite
     * @param nPages Numero di pagine del docoumento
     * @throws IllegalArgumentException Il documento ha troppe pagine
     */
    private void validatePages(int nPages) throws IllegalArgumentException {
        if (nPages > MAX_PAGES) {
            logger.warn("File con troppe pagine: {}", nPages);
            throw new IllegalArgumentException("Il file ha troppe pagine");
        }
    }


}