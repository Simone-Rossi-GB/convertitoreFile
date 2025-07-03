package webService.server.converters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import webService.server.config.configHandlers.conversionContext.ConversionContextReader;
import webService.server.Utility;

import java.io.File;

public abstract class Converter {
    private static final Logger logger = LogManager.getLogger(Converter.class);

    public File conversione(File srcFile) throws Exception {
        logger.info(srcFile.getAbsolutePath());
        // Conversione base
        File outFile = convert(srcFile);
        logger.info(outFile.getAbsolutePath());
        // Controlla se il file in output deve essere zippato
        if(ConversionContextReader.getIsZippedOutput() && !Utility.getExtension(outFile).equals("zip")) {
            if (ConversionContextReader.getIsUnion())
                return Zipper.compressioneFileProtetto(outFile, Utility.getBaseName(srcFile));
            else
                return Zipper.compressioneFile(outFile, Utility.getBaseName(srcFile));
        }
        return outFile;
    }

    public abstract File convert(File srcFile) throws Exception;
}
