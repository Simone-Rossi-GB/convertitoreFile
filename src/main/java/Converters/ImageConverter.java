package Converters;

import com.itextpdf.text.DocumentException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class ImageConverter implements Converter {

    @Override
    public ArrayList<File> convert(File imgFile) throws IOException, DocumentException {
        System.out.println("ciao bello");
        ArrayList<File> files = new ArrayList<>();
        String imgName = imgFile.getName();
        Pattern pattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = pattern.matcher(imgName);

        if (matcher.find()) {
            String extracted = matcher.group(1);
            File file = imageConversion(imgFile, extracted);
            files.add(file);
        } else {
            System.out.println("No match found in file name: " + imgName);
        }

        return files;
    }

    public static File imageConversion(File imgFile, String extracted) throws IOException {
        List<String> estensioniTrasparenza = new ArrayList<>(Arrays.asList("png", "TIFF", "WebP"));
        BufferedImage image = ImageIO.read(imgFile);

        if (image == null) {
            throw new IOException("Immagine non valida o non supportata: " + imgFile.getName());
        }

        File outFile = new File("src/temp", getName(imgFile) + "." + extracted);
        int lastDotIndex = imgFile.getName().lastIndexOf('.');
        String extension = imgFile.getName().substring(lastDotIndex + 1);

        if (!estensioniTrasparenza.contains(extension) && !estensioniTrasparenza.contains(extracted)) {
            image = alphaChannelRemover(image);
        }

        ImageIO.write(image, extracted, outFile);
        return outFile;
    }

    private static String getName(File inputFile) {
        String temp = inputFile.getName();
        return temp.split("\\.")[0];
    }

    private static BufferedImage alphaChannelRemover(BufferedImage inImage) {
        int imageType = BufferedImage.TYPE_INT_RGB;
        BufferedImage copy = new BufferedImage(inImage.getWidth(), inImage.getHeight(), imageType);
        Graphics2D g2d = copy.createGraphics();
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.drawImage(inImage, 0, 0, Color.WHITE, null);
        g2d.dispose();
        return copy;
    }
}

