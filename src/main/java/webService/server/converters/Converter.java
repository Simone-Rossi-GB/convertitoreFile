package webService.server.converters;

import webService.server.configuration.configHandlers.conversionContext.ConversionContextReader;
import webService.client.objects.Utility;
import java.io.File;

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
