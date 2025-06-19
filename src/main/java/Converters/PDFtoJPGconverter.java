package Converters;

import converter.Log;
import converter.Utility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class PDFtoJPGconverter extends AbstractPDFConverter {
    private static final int DPI = 300; // DPI dell'immagine renderizzata

    /**
     * Metodo per unire le pagine del pdf una sotto l'altra in un'unica immagine
     * @param images Immagini relative alle singole pagine del pdf
     * @return BufferedImage con tutte le pagine del pdf
     */
    private BufferedImage mergeImagesVertically(ArrayList<BufferedImage> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("L'oggetto images non esiste o è vuoto");
        }

        int width = 0;
        int totalHeight = 0;

        for (BufferedImage img : images) {
            if (img == null) continue; // ignora eventuali immagini null
            width = Math.max(width, img.getWidth());
            totalHeight += img.getHeight();
        }

        BufferedImage combined = new BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = combined.getGraphics();

        int y = 0;
        for (BufferedImage img : images) {
            if (img == null) continue; // ignora eventuali immagini null
            g.drawImage(img, 0, y, null);
            y += img.getHeight();
        }

        g.dispose();
        return combined;
    }

    /**
     * Conversione pdf -> jpg
     * @param pdfFile File di partenza
     * @param pdfDocument Documento pdf caricato
     * @param union Boolean che indica se unire o no le pagine in un'unica immagine
     * @return ArrayList di file convertiti
     * @throws Exception Errore durante il processo di conversione
     */
    @Override
    public ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument, boolean union) throws Exception {
        validateInputs(pdfFile, pdfDocument);
        Log.addMessage("[PDFtoJPG] Conversione iniziata con parametri:\n" +
                "| pdfFile.getPath() = " + pdfFile.getPath()+"\n" +
                "| union = " + union);
        try{
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            ArrayList<BufferedImage> images = new ArrayList<>();
            ArrayList<File> outputFiles = new ArrayList<>();

            String baseName = Objects.requireNonNull(pdfFile.getName().replaceAll("(?i)\\.pdf$", "")); // senza estensione
            for (int i = 0; i < pdfDocument.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI);
                images.add(image);

                if (!union) {
                    File tempFile = new File(baseName + "_page_" + (i + 1) + ".jpg");
                    ImageIO.write(image, "jpg", tempFile);
                    outputFiles.add(tempFile);
                }
            }

            if (union) {
                BufferedImage mergedImage = mergeImagesVertically(images);
                File mergedFile = new File(baseName + ".jpg");  // usa il nome del PDF
                ImageIO.write(mergedImage, "jpg", mergedFile);
                outputFiles.add(mergedFile);
            }
            pdfDocument.close();
            if (outputFiles.size() > 1){
                Log.addMessage("Compressione delle immagini generate in output");
                File zippedImages = Utility.zipImages(outputFiles);
                outputFiles.clear();
                outputFiles.add(zippedImages);
            }
            return outputFiles;
        }catch (Exception e){
            throw new Exception("Errore durante il processo di conversione: " + e.getMessage());
        } finally {
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }
    }

}