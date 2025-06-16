package Converters;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public interface Converter {
    ArrayList<File> convert(File srcFile) throws IOException;
}
