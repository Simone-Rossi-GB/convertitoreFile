package webService.server.converters.imageConverters;

import webService.server.configuration.configExceptions.NullJsonValueException;
import webService.server.configuration.configHandlers.conversionContext.ConversionContextReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public interface ImageConverterUtility {
    Logger logger = LogManager.getLogger(ImageConverterUtility.class);

    static String getAndCheckOutputExtension() throws NullJsonValueException {
        String ext = ConversionContextReader.getDestinationFormat();
        if (ext != null) {
            return ext;
        } else {
            logger.error("Il destinationFormat in converionContext e null");
            throw new NullJsonValueException("Il destinationFormat in converionContext e null");
        }

    }

    /**
     * Estrae l'estensione del file in minuscolo
     */
    static String getExtension(File inputFile) {
        String fileName = inputFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }

    /**
     * Rimuove il canale alpha da un'immagine, riempiendo con sfondo bianco
     */
    static BufferedImage removeAlphaChannel(BufferedImage inImage) {
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


    File convert(File imgFile) throws NullJsonValueException, IOException;
}
