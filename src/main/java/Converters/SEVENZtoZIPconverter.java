package Converters;

import org.apache.commons.compress.archivers.sevenz.*;
import org.apache.commons.compress.archivers.zip.*;
import java.io.*;
import java.util.*;

public class SEVENZtoZIPconverter implements Converter{
    @Override
    public ArrayList<File> convert(File srcFile) throws Exception {
        File outFile = new File("src/temp/");
        try (SevenZFile sevenZFile = new SevenZFile(srcFile);
             ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(outFile)) {
            SevenZArchiveEntry entry;
            while ((entry = sevenZFile.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                ZipArchiveEntry zipEntry = new ZipArchiveEntry(entry.getName());
                zipOut.putArchiveEntry(zipEntry);

                byte[] buffer = new byte[(int) entry.getSize()];
                sevenZFile.read(buffer, 0, buffer.length);
                zipOut.write(buffer);
                zipOut.closeArchiveEntry();
            }

            zipOut.finish();
        }
        return new ArrayList<File>() {{
            add(outFile);
        }};
    }
}
