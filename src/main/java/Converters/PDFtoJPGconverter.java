package Converters;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PDFtoJPGconverter {
    private static final int DPI = 300; // DPI of rendered image
    public static ArrayList<File> convert(File inputFile) throws IOException {
        PDDocument document = PDDocument.load(inputFile);
        PDFRenderer renderer = new PDFRenderer(document);
        ArrayList<File> outputFiles = new ArrayList<>(document.getNumberOfPages());
        for (int i = 0; i<document.getNumberOfPages(); i++){
            BufferedImage image = renderer.renderImageWithDPI(i, DPI);
            File tempFileStore = new File("output_" + (i + 1) + ".jpg");
            ImageIO.write(image, "jpg", tempFileStore);
            outputFiles.add(tempFileStore);
        }
        document.close();
        System.out.println("Conversion completed!");
        return outputFiles;
    }
}
