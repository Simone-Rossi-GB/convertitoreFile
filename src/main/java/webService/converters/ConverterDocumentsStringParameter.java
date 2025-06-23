package webService.converters;

import gui.MainViewController;

import java.io.File;
import java.util.ArrayList;

public abstract class ConverterDocumentsStringParameter implements Converter {
    @Override
    public ArrayList<File> convert(File srcFile) throws Exception{
        String extraParameter = MainViewController.launchDialogStringParameter();
        return convertProtectedFile(srcFile, extraParameter);
    }

    public abstract ArrayList<File> convertProtectedFile(File srcFile, String extraParameter) throws Exception;
}
