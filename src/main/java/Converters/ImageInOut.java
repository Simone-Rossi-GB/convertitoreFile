package Converters;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ImageInOut {
    public static String convertiFile(String srcPath, String inExt, String outExt , String outPath) throws IOException {
        File inputFile = new File(srcPath);
        System.out.println(inputFile.getName());

        BufferedImage image = ImageIO.read(inputFile);
        System.out.println(image);
        if (image == null){
            throw new IOException("Formato immagine non supportato o file corrotto");
        }
        File outFile = new File(outPath, getName(inputFile) + "." + outExt);
        // Preparazione dell'immagine alla conversione
        if (outExt.equals("png")){
            //image = colorCodecSwitcher(image);
        }
        if (inExt.equals("png")){
            image = alphaChannelRemover(image);
            //image = colorCodecSwitcher(image);
        }

        boolean written = ImageIO.write(image, outExt, outFile);
        System.out.println("TEST3");
        if (written){
            System.out.println("File convertito creato correttamente");
        } else {
            System.out.println("Errore durante la creazione del file");
        }
        return outFile.getPath();
    }
    private static String getName(File inputFile){
        String temp = inputFile.getName();
        return temp.split("\\.")[0];
    }
    private static BufferedImage alphaChannelRemover(BufferedImage inImage){
        int colorComponent = inImage.getType();
        switch (colorComponent){
            case BufferedImage.TYPE_INT_ARGB:
                colorComponent = BufferedImage.TYPE_INT_RGB;
            case BufferedImage.TYPE_4BYTE_ABGR:
                colorComponent = BufferedImage.TYPE_INT_BGR;
        }
        // Creo una copia di inImage per l'output
        BufferedImage copy = new BufferedImage(inImage.getWidth(), inImage.getHeight(), colorComponent);
        Graphics2D g2d = copy.createGraphics();
        g2d.drawImage(inImage, 0, 0, null);
        g2d.dispose();
        return copy;
    }
}
