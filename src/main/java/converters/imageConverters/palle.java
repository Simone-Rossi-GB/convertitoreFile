package converters.imageConverters;

import configuration.configHandlers.config.ConfigReader;
import configuration.configHandlers.conversionContext.ConversionContextReader;
import converters.Converter;
import net.ifok.image.image4j.codec.ico.ICODecoder;
import net.ifok.image.image4j.codec.ico.ICOEncoder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Convertitore per immagini tra vari formati (png, jpg, ico, webp, ecc.)
 */
public class palle extends Converter {
    private static final Logger logger = LogManager.getLogger(palle.class);
    /**
     * Converte un'immagine nel formato desiderato, dedotto dal nome del file
     */
    @Override
    public File convert(File imgFile) throws IOException {
        String estensione = ConversionContextReader.getDestinationFormat();
        File outputFile = null;
        if (estensione != null) {
            try {
                outputFile = imageConversion(imgFile, estensione);
            } catch (IOException e) {
                logger.error("Conversione immagine fallita per:{}", imgFile.getName());
                throw e;
            }
        } else {
            logger.error("Formato immagine non riconosciuto nel nome del file {}", imgFile.getName());
        }

        return outputFile;
    }

    /**
     * Metodo principale per la conversione dell'immagine nel formato desiderato
     */
    public static File imageConversion(File imgFile, String targetFormat) throws IOException {
        logger.info("Inizio conversione immagine:\n | {} -> .{}", imgFile.getName(), targetFormat);

        List<String> formatsWithAlpha = ConfigReader.getFormatsWithAlphaChannel();
        List<String> formatsRequiringIntermediate = ConfigReader.getFormatsRequiringIntermediateConversion();

        String originalExtension = getExtension(imgFile);
        File outFile;

        BufferedImage image;

        if (originalExtension.equals("ico")) {
            List<BufferedImage> images = ICODecoder.read(imgFile);
            if (images.isEmpty()) {
                logger.error("Nessuna immagine valida trovata nel file ICO.");
                throw new IOException("File ICO non valido: " + imgFile.getName());
            }

            // Seleziona l'immagine con risoluzione più alta
            BufferedImage largest = images.stream()
                    .max(Comparator.comparingInt(img -> img.getWidth() * img.getHeight())).get();

            if (formatsRequiringIntermediate.contains(targetFormat)) {
                outFile = new File("src/temp", getBaseName(imgFile) + ".png");
                ImageIO.write(largest, "png", outFile);
            }

            outFile = new File("src/temp", getBaseName(imgFile) + "." + targetFormat);
            ICOEncoder.write(largest, outFile);
            logger.info("Creazione file .{} completata: {}", targetFormat, outFile.getName());
            return outFile;

        } else {
            image = ImageIO.read(imgFile);
            if (image == null) {
                logger.error("Lettura immagine fallita - formato non supportato o file corrotto.");
                throw new IOException("Immagine non valida: " + imgFile.getName());
            }
        }

        // Rimuove trasparenza se necessario
        if (formatsWithAlpha.contains(originalExtension) ^ formatsWithAlpha.contains(targetFormat.toLowerCase())) {
            image = removeAlphaChannel(image);
        }


        outFile = new File("src/temp", getBaseName(imgFile) + "." + targetFormat);
        logger.info("File temporaneo creato correttamente");

        if (targetFormat.equalsIgnoreCase("ico")) {
            ICOEncoder.write(image, outFile);
        } else {
            ImageIO.write(image, targetFormat, outFile);
            logger.info("File in uscita scritto correttamente");
        }

        logger.info("Creazione file .{} completata: {}", targetFormat, outFile.getName());
        return outFile;
    }

    /**
     * Estrae il nome del file senza estensione
     */
    private static String getBaseName(File inputFile) {
        String fileName = inputFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(0, dotIndex) : fileName;
    }

    /**
     * Estrae l'estensione del file in minuscolo
     */
    private static String getExtension(File inputFile) {
        String fileName = inputFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }

    /**
     * Rimuove il canale alpha da un'immagine, riempiendo con sfondo bianco
     */
    private static BufferedImage removeAlphaChannel(BufferedImage inImage) {
        logger.info("Rimozione canale alpha da immagine");

        BufferedImage copy = new BufferedImage(
                inImage.getWidth(), inImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = copy.createGraphics();
        g2d.setComposite(AlphaComposite.SrcOver);
        g2d.drawImage(inImage, 0, 0, Color.WHITE, null);
        g2d.dispose();

        return copy;
    }
}