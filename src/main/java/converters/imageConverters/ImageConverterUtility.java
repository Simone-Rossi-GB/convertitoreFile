package converters.imageConverters;

import configuration.configExceptions.NullJsonValueException;
import configuration.configHandlers.conversionContext.ConversionContextReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

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
    static String getExtension(File inputFile) {
        String fileName = inputFile.getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex > 0) ? fileName.substring(dotIndex + 1).toLowerCase() : "";
    }
}
