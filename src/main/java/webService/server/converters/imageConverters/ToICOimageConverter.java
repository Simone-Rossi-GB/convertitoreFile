package webService.server.converters.imageConverters;

import webService.server.config.configExceptions.NullJsonValueException;
import webService.server.config.configHandlers.Config;
import webService.server.config.configHandlers.serverConfig.ConfigReader;
import webService.server.converters.Converter;
import net.ifok.image.image4j.codec.ico.ICOEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static webService.server.Utility.getBaseName;
import static webService.server.converters.imageConverters.ImageConverterUtility.removeAlphaChannel;

public class ToICOimageConverter extends Converter {

    private static final Logger logger = LogManager.getLogger(ToICOimageConverter.class);

    @Override
    public File convert(File imgFile, Config configuration) throws NullJsonValueException, IOException {
        String outputExtension = ImageConverterUtility.getAndCheckOutputExtension(configuration);
        List<String> formatsWithAlpha = ConfigReader.getFormatsWithAlphaChannel();

        logger.info("Inizio conversione immagine:\n | {} -> .{}", imgFile.getName(), outputExtension);

        String originalExtension = ImageConverterUtility.getExtension(imgFile);
        File outFile;
        BufferedImage image;

        image = ImageIO.read(imgFile);
        if (image == null) {
            logger.error("Lettura immagine fallita - formato non supportato o file corrotto.");
            throw new IOException("Immagine non valida: " + imgFile.getName());
        }

        // Rimuove trasparenza se necessario
        if (formatsWithAlpha.contains(originalExtension) ^ formatsWithAlpha.contains(outputExtension.toLowerCase())) {
            image = removeAlphaChannel(image);
        }

        outFile = new File(imgFile.getParent(), getBaseName(imgFile) + "." + outputExtension);
        logger.info("File temporaneo creato correttamente");

        ICOEncoder.write(image, outFile);
        logger.info("File in uscita scritto correttamente");

        logger.info("Creazione file .{} completata: {}", outputExtension, outFile.getName());
        return outFile;
    }
}
