package Converters;

import com.itextpdf.text.DocumentException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public interface Converter {
    ArrayList<File> convert(File srcFile) throws IOException, DocumentException;
}
