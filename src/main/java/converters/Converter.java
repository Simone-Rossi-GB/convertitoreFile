package converters;

import configuration.configHandlers.conversionContext.ConversionContextReader;
import converter.Utility;
import java.io.File;

public abstract class Converter {

    public File conversione(File srcFile) throws Exception {
        File outFile = convert(srcFile);
        if(ConversionContextReader.getIsZippedOutput()/* && !Utility.getExtension(outFile).equals("zip")*/) {
            return Zipper.compressioneFile(outFile, Utility.getBaseName(srcFile));
        }
        return outFile;
    }

    public abstract File convert(File srcFile) throws Exception;
}
