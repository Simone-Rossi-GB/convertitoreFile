package Converters;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PDFtoJPGconverter implements Converter {
    private static final int DPI = 300; // DPI dell'immagine renderizzata

    @Override
    public ArrayList<File> convert(File inputFile, boolean unisci) throws IOException {
        PDDocument document = PDDocument.load(inputFile);
        PDFRenderer renderer = new PDFRenderer(document);
        ArrayList<BufferedImage> images = new ArrayList<>();
        ArrayList<File> outputFiles = new ArrayList<>();

        String baseName = inputFile.getName().replaceAll("(?i)\\.pdf$", ""); // senza estensione

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, DPI);
            images.add(image);

            if (!unisci) {
                File tempFile = new File(baseName + "_page_" + (i + 1) + ".jpg");
                ImageIO.write(image, "jpg", tempFile);
                outputFiles.add(tempFile);
            }
        }

        if (unisci) {
            BufferedImage mergedImage = mergeImagesVertically(images);
            File mergedFile = new File(baseName + ".jpg");  // usa il nome del PDF
            ImageIO.write(mergedImage, "jpg", mergedFile);
            outputFiles.add(mergedFile);
        }

        document.close();
        System.out.println("Conversion completed!");
        return outputFiles;
    }

    private BufferedImage mergeImagesVertically(ArrayList<BufferedImage> images) {
        int width = 0;
        int totalHeight = 0;

        for (BufferedImage img : images) {
            width = Math.max(width, img.getWidth());
            totalHeight += img.getHeight();
        }

        BufferedImage combined = new BufferedImage(width, totalHeight, BufferedImage.TYPE_INT_RGB);
        Graphics g = combined.getGraphics();

        int y = 0;
        for (BufferedImage img : images) {
            g.drawImage(img, 0, y, null);
            y += img.getHeight();
        }

        g.dispose();
        return combined;
    }
}


