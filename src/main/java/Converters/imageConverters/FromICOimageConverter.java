package converters.imageConverters;

import webService.configuration.configExceptions.NullJsonValueException;
import webService.configuration.configHandlers.config.ConfigReader;
import converters.Converter;
import net.ifok.image.image4j.codec.ico.ICODecoder;
import net.ifok.image.image4j.codec.ico.ICOEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import static objects.Utility.getBaseName;
import static converters.imageConverters.ImageConverterUtility.removeAlphaChannel;

public class FromICOimageConverter extends Converter {
    private static final Logger logger = LogManager.getLogger(FromICOimageConverter.class);
    @Override
    public File convert(File imgFile) throws NullJsonValueException, IOException {
        String outputExtension = ImageConverterUtility.getAndCheckOutputExtension();
        List<String> formatsWithAlpha = ConfigReader.getFormatsWithAlphaChannel();
        List<String> formatsRequiringIntermediate = ConfigReader.getFormatsRequiringIntermediateConversion();

        logger.info("Inizio conversione immagine:\n | {} -> .{}", imgFile.getName(), outputExtension);

        File outFile;

        List<BufferedImage> images = ICODecoder.read(imgFile);

        if (images.isEmpty()) {
            logger.error("Nessuna immagine valida trovata nel file ICO.");
            throw new IOException("File ICO non valido: " + imgFile.getName());
        }

        // Seleziona l'immagine con risoluzione più alta
        BufferedImage largest = images.stream()
                .max(Comparator.comparingInt(img -> img.getWidth() * img.getHeight())).get();

        // Rimuove trasparenza se necessario
        if(!formatsWithAlpha.contains(outputExtension)){
            largest = removeAlphaChannel(largest);
        }

        if (formatsRequiringIntermediate.contains(outputExtension)) {
            outFile = new File("src/temp", getBaseName(imgFile) + ".png");
            ImageIO.write(largest, "png", outFile);
        }

        outFile = new File("src/temp", getBaseName(imgFile) + "." + outputExtension);
        ICOEncoder.write(largest, outFile);

        logger.info("Creazione file .{} completata: {}",outputExtension, outFile.getName());
        return outFile;
    }
}
