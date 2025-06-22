package Converters;
import configuration.configHandlers.conversionContext.ConversionContext;

import java.io.File;
import java.util.ArrayList;

public abstract class ConverterDocumentsStringParameter implements Converter{
    @Override
    public ArrayList<File> convert(File srcFile) throws Exception{
        String extraParameter = (String) ConversionContext.get("password");
        return convertProtectedFile(srcFile, extraParameter);
    }

    public abstract ArrayList<File> convertProtectedFile(File srcFile, String extraParameter) throws Exception;
}
