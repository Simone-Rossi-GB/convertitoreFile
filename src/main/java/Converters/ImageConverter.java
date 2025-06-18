package Converters;

import com.itextpdf.text.DocumentException;
import net.ifok.image.image4j.codec.ico.ICODecoder;
import net.ifok.image.image4j.codec.ico.ICOEncoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

public class ImageConverter implements Converter {

    @Override
    public ArrayList<File> convert(File imgFile) throws IOException, DocumentException {
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
        List<String> estensioniTrasparenza = Arrays.asList("png", "tiff", "gif", "webp", "psd", "icns", "ico", "tga", "iff");
        List<String> estensioniConConvIntermedia = Arrays.asList("webp", "pbm", "pgm", "ppm", "pam", "tga", "iff", "xwd", "icns", "pnm");
        File outFile;
        int lastDotIndex = imgFile.getName().lastIndexOf('.');
        String extension = imgFile.getName().substring(lastDotIndex + 1).toLowerCase();

        BufferedImage image = null;
        List<BufferedImage> images = null;

        System.out.println("Lettura Immagine ...");
        if (extension.equals("ico")){
            images = ICODecoder.read(imgFile);
            BufferedImage largest = images.stream()
                    .max(Comparator.comparingInt(img -> img.getWidth() * img.getHeight()))
                    .orElse(images.get(0));
            if (estensioniConConvIntermedia.contains(extension)){
                outFile = new File("src/temp", getName(imgFile) + ".png");
                ImageIO.write(largest, "png", outFile);
            }
            outFile = new File("src/temp", getName(imgFile) + "." + extracted);
            ICOEncoder.write(largest, outFile);
            return outFile;
        }else {
            image = ImageIO.read(imgFile);
        }

        if (image == null) {
            throw new IOException("Immagine non valida o non supportata: " + imgFile.getName());
        }



        if (!estensioniTrasparenza.contains(extension) || !estensioniTrasparenza.contains(extracted.toLowerCase())) {
            image = alphaChannelRemover(image);

        }
        System.out.println("Scrittura Immagine ...");
        if (extracted.equals("ico")) {
            outFile = new File("src/temp", getName(imgFile) + "." + extracted);
            ICOEncoder.write(image, outFile);
            return outFile;
        }
        outFile = new File("src/temp", getName(imgFile) + "." + extracted);
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
    public static BufferedImage resize(BufferedImage originalImage, int width, int height) {
        Image tmp = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH); // Altri: SCALE_FAST, SCALE_AREA_AVERAGING
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return resized;
    }
}
