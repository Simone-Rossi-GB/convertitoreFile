package Converters;

import com.itextpdf.text.DocumentException;
import converter.Log;
import converter.Utility;
import net.ifok.image.image4j.codec.ico.ICODecoder;
import net.ifok.image.image4j.codec.ico.ICOEncoder;
import org.apache.commons.logging.LogFactory;

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

    private static final org.apache.commons.logging.Log log = LogFactory.getLog(ImageConverter.class);

    @Override
    public ArrayList<File> convert(File imgFile) throws IOException, DocumentException {
        ArrayList<File> files = new ArrayList<>();
        String extracted;
        if ((extracted = Utility.estraiEstensioneInterna(imgFile)) != null){
            File file = imageConversion(imgFile, extracted);
            files.add(file);
        } else {
            Log.addMessage("Formato nome immagine errato");
        }
        return files;
    }

    public static File imageConversion(File imgFile, String extracted) throws IOException {
        Log.addMessage("Inizio conversione immagine: "+Utility.estraiNomePiuEstensioneFile(imgFile)+" -> ."+extracted);
        List<String> estensioniTrasparenza = Arrays.asList("png", "tiff", "gif", "webp", "psd", "icns", "ico", "tga", "iff");
        List<String> estensioniConConvIntermedia = Arrays.asList("webp", "pbm", "pgm", "ppm", "pam", "tga", "iff", "xwd", "icns", "pnm");
        File outFile;
        int lastDotIndex = imgFile.getName().lastIndexOf('.');
        String extension = imgFile.getName().substring(lastDotIndex + 1).toLowerCase();

        BufferedImage image;
        List<BufferedImage> images;

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
            Log.addMessage("Errore lettura immagine");
            throw new IOException("Immagine non valida: " + imgFile.getName());
        }
        if (estensioniTrasparenza.contains(extension) && estensioniTrasparenza.contains(extracted.toLowerCase())) {
            image = alphaChannelRemover(image);
        }
        outFile = new File("src/temp", getName(imgFile) + "." + extracted);
        if (extracted.equals("ico")) {
            ICOEncoder.write(image, outFile);
        } else {
            ImageIO.write(image, extracted, outFile);
        }
        Log.addMessage("Creazione file ."+extracted+" completata: "+Utility.estraiNomePiuEstensioneFile(outFile));
        return outFile;
    }

    private static String getName(File inputFile) {
        String temp = inputFile.getName();
        return temp.split("\\.")[0];
    }

    private static BufferedImage alphaChannelRemover(BufferedImage inImage) {
        Log.addMessage("Rimozione canale alpha da immagine");
        int imageType = BufferedImage.TYPE_INT_RGB;
        BufferedImage copy = new BufferedImage(inImage.getWidth(), inImage.getHeight(), imageType);
        Graphics2D g2d = copy.createGraphics();
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.drawImage(inImage, 0, 0, Color.WHITE, null);
        g2d.dispose();
        return copy;
    }
}
