package webService.server.converters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import webService.server.config.configHandlers.Config;
import webService.server.Utility;

import java.io.File;

public abstract class Converter {
    private static final Logger logger = LogManager.getLogger(Converter.class);

    public File conversione(File srcFile, Config configuration) throws Exception {
        logger.info(srcFile.getAbsolutePath());
        // Conversione base
        File outFile = convert(srcFile, configuration);
        logger.info(outFile.getAbsolutePath());
        // Controlla se il file in output deve essere zippato
        if(configuration.getData().isZippedOutput() && !Utility.getExtension(outFile).equals("zip")) {
           logger.info(configuration.getData().isProtectedOutput());
            if (configuration.getData().isProtectedOutput())
                {
                logger.info("Compressione protetta");
                return Zipper.compressioneFileProtetto(outFile, Utility.getBaseName(srcFile), configuration.getData().getPassword());
            }
            else
                return Zipper.compressioneFile(outFile, Utility.getBaseName(srcFile));
        }
        return outFile;
    }

    public abstract File convert(File srcFile, Config configuration) throws Exception;
}
