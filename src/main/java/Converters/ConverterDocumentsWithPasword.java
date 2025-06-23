package Converters;
import configuration.configHandlers.conversionContext.ConversionContextReader;
import converter.DirectoryWatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;

public abstract class ConverterDocumentsWithPasword implements Converter{
    private static final Logger logger = LogManager.getLogger(DirectoryWatcher.class);
    /**
     * Metodo che prende il parametro Password dal JSON per tutti quei formati che la possono richiedere
     * @param srcFile File di partenza
     * @return file convertito
     * @throws Exception
     */
    @Override
    public File convert(File srcFile) throws Exception{
        String extraParameter = ConversionContextReader.getPassword();
        logger.info("Password rilevata: " + extraParameter);
        return convertProtectedFile(srcFile, extraParameter);
    }

    /**
     * Metodo astratto che si occupa di caricare i documenti dei vari formati con o senza password, a seconda del contesto
     * @param srcFile File di partenza
     * @param password Password letta dal JSON
     * @return file convertito
     * @throws Exception
     */
    public abstract File convertProtectedFile(File srcFile, String password) throws Exception;
}
