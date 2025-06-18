package Converters;

import com.itextpdf.text.DocumentException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public interface Converter {
    default ArrayList<File> convert(File srcFile) throws Exception {
        return null;
    }
    default ArrayList<File> convert(File srcFile, String password) throws Exception {
        return null;
    }
    default ArrayList<File> convert(File srcFile, boolean opzioni) throws Exception{
        return null;
    }

    default ArrayList<File> convert(File srcFile, String password, boolean opzioni) throws Exception{
        return null;
    }
}
