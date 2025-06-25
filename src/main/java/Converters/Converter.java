package converters;

import converters.exception.ConversionException;
import converters.exception.FileMoveException;
import webService.configuration.configHandlers.conversionContext.ConversionContextReader;
import objects.Utility;
import java.io.File;
import java.io.IOException;

public abstract class Converter {

    public File conversione(File srcFile) throws Exception {
        File outFile = convert(srcFile);
        if(ConversionContextReader.getIsZippedOutput() && !Utility.getExtension(outFile).equals("zip")) {
            System.out.println("Zippo i file");
            return Zipper.compressioneFile(outFile, Utility.getBaseName(srcFile));
        }
        return outFile;
    }

    public abstract File convert(File srcFile) throws Exception;
}
