package Converters;
import configuration.configHandlers.conversionContext.ConversionContextReader;

import java.io.File;
import java.util.ArrayList;

public abstract class ConverterDocumentsStringParameter implements Converter{
    @Override
    public ArrayList<File> convert(File srcFile) throws Exception{
        String extraParameter = ConversionContextReader.getPassword();
        return convertProtectedFile(srcFile, extraParameter);
    }

    public abstract ArrayList<File> convertProtectedFile(File srcFile, String extraParameter) throws Exception;
}
