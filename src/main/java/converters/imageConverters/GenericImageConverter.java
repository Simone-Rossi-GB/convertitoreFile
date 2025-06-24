package converters.imageConverters;

import configuration.configExceptions.NullJsonValueException;
import configuration.configHandlers.config.ConfigReader;
import converters.Converter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class GenericImageConverter extends Converter {
    private static final Logger logger = LogManager.getLogger(GenericImageConverter.class);
    @Override
    public File convert(File imgFile) throws NullJsonValueException {
        String outputExtension = ImageConverterUtility.getAndCheckOutputExtension();
        List<String> formatsWithAlpha = ConfigReader.getFormatsWithAlphaChannel();

        logger.info("Inizio conversione immagine:\n | {} -> .{}", imgFile.getName(), outputExtension);

        String originalExtension = ImageConverterUtility.getExtension(imgFile);
        File outFile;
        BufferedImage image;

        // Rimuove trasparenza se necessario
        if (formatsWithAlpha.contains(originalExtension) ^ formatsWithAlpha.contains(targetFormat.toLowerCase())) {
            image = removeAlphaChannel(image);
        }
    }
}
