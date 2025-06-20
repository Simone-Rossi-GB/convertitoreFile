package Converters;

import converter.Log;
import converter.Utility;
import gui.MainViewController;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Objects;

public class PDFtoJPGconverter extends AbstractPDFConverter {

    private static final Logger logger = LogManager.getLogger(PDFtoJPGconverter.class);
    private static final int DPI = 300; // DPI dell'immagine renderizzata
    private static final int MAX_PAGES = 3; // numero massimo di pagine

    @Override
    public boolean supportsBooleanOption(File srcFile) {
        return true; // Supporta l'opzione di unione delle immagini
    }

    @Override
    public String getBooleanOptionDescription() {
        return "Unire le pagine in un'unica immagine JPG";
    }

    /**
     * Metodo per unire le pagine del pdf una sotto l'altra in un'unica immagine
     *
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
     * Conversione pdf -> jpg con opzione di unione
     *
     * @param pdfFile     File di partenza
     * @param pdfDocument Documento pdf caricato
     * @param union       Se true, unisce tutte le pagine in un'unica immagine
     * @return ArrayList di file convertiti
     * @throws Exception Errore durante il processo di conversione
     */
    @Override
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument, boolean union) throws Exception {
        validateInputs(pdfFile, pdfDocument);

        // Se siamo nell'ambiente GUI e union è false (default), chiedi all'utente
        if (ConverterContext.isGuiEnvironment() && !union) {
            try {
                union = MainViewController.launchDialogUnisci();
            } catch (Exception e) {
                logger.warn("Impossibile mostrare dialogo unione, uso valore default: false");
                Log.addMessage("Impossibile mostrare dialogo unione, uso valore default: false");
                union = false;
            }
        }

        logger.info("Inizio conversione con parametri: \n | pdfFile.getPath() = {}, union={}", pdfFile.getPath(), union);
        Log.addMessage("Conversione iniziata con parametri:\n" +
                "| pdfFile.getPath() = " + pdfFile.getPath() + "\n" +
                "| union = " + union);

        int nPages = pdfDocument.getNumberOfPages();

        try {
            PDFRenderer renderer = new PDFRenderer(pdfDocument);
            ArrayList<BufferedImage> images = new ArrayList<>();
            ArrayList<File> outputFiles = new ArrayList<>();

            String baseName = Objects.requireNonNull(pdfFile.getName().replaceAll("(?i)\\.pdf$", ""));

            for (int i = 0; i < nPages; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, DPI);
                images.add(image);

                if (!union) {
                    File tempFile = new File(baseName + "_page_" + (i + 1) + ".jpg");
                    ImageIO.write(image, "jpg", tempFile);
                    outputFiles.add(tempFile);
                }
            }

            if (union) {
                validatePages(nPages);
                BufferedImage mergedImage = mergeImagesVertically(images);
                File mergedFile = new File(baseName + ".jpg");  // usa il nome del PDF
                ImageIO.write(mergedImage, "jpg", mergedFile);
                outputFiles.add(mergedFile);
            }

            if (outputFiles.size() > 1) {
                Log.addMessage("Compressione delle immagini generate in output");
                File zippedImages = Utility.zipper(outputFiles);
                zippedImages = rinominaFileZip(zippedImages, baseName);
                outputFiles.clear();
                outputFiles.add(zippedImages);
            }

            logger.info("Conversione completata, {} file prodotti", outputFiles.size());
            Log.addMessage("Conversione completata, " + outputFiles.size() + " file prodotti");
            return outputFiles;

        } catch (Exception e) {
            logger.error("Durante il processo di conversione: {}", e.getMessage(), e);
            Log.addMessage("ERRORE: Durante il processo di conversione: " + e.getMessage());
            throw new Exception("Errore durante il processo di conversione: " + e.getMessage(), e);
        }
    }

    /**
     * Controlla se il numero di pagine del documento rientra nel limite
     * @param nPages Numero di pagine del documento
     * @throws Exception Il documento ha troppe pagine
     */
    private void validatePages(int nPages) throws Exception {
        if (nPages > MAX_PAGES) {
            logger.warn("File con troppe pagine: {}", nPages);
            Log.addMessage("ERRORE: File con troppe pagine: " + nPages);
            throw new Exception("Il file ha troppe pagine (" + nPages + "). Massimo consentito: " + MAX_PAGES);
        }
    }

    /**
     * Rinomina il file ZIP generato
     * @param zipFile File ZIP da rinominare
     * @param name Nuovo nome base
     * @return File ZIP rinominato
     * @throws Exception Errore durante la rinomina
     */
    private File rinominaFileZip(File zipFile, String name) throws Exception {
        File renamedZip = new File(zipFile.getParent(), name + ".zip");
        try {
            Files.move(zipFile.toPath(), renamedZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.info("File ZIP rinominato: {} -> {}", zipFile.getName(), renamedZip.getName());
            Log.addMessage("File ZIP rinominato: " + zipFile.getName() + " -> " + renamedZip.getName());
            return renamedZip;
        } catch (IOException e) {
            logger.error("Impossibile rinominare il file ZIP: {}", e.getMessage(), e);
            Log.addMessage("ERRORE: Impossibile rinominare il file ZIP: " + e.getMessage());
            throw new Exception("Impossibile rinominare il file ZIP: " + e.getMessage(), e);
        }
    }
}