package webService.converters;

import java.io.File;
import java.util.ArrayList;

public interface Converter {
    public ArrayList<File> convert(File srcFile) throws Exception;
    /*default ArrayList<File> convert(File srcFile, String parameter) throws Exception {
        return null;
    }
    default ArrayList<File> convert(File srcFile, boolean opzioni) throws Exception{
        return null;
    }

    default ArrayList<File> convert(File srcFile, String password, boolean opzioni) throws Exception{
        return null;
    }*/

}
