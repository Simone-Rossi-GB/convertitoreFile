package Converters;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.util.ArrayList;

public class TARGZtoZIPconverter implements Converter {
    private void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
    }

    @Override
    public ArrayList<File> convert(File tarGzFile) throws IOException {
        ArrayList<File> outputFiles = new ArrayList<>();

        String name = tarGzFile.getName();
        int lastDot = name.lastIndexOf('.');
        String baseName = (lastDot == -1) ? name : name.substring(0, lastDot);
        File zipFile = new File(tarGzFile.getParent(), baseName + ".zip");

        try (FileInputStream fis = new FileInputStream(tarGzFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gis);
             FileOutputStream fos = new FileOutputStream(zipFile);
             ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(fos)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                ZipArchiveEntry zipEntry = new ZipArchiveEntry(entry.getName());
                zipOut.putArchiveEntry(zipEntry);
                if (!entry.isDirectory()) {
                    copy(tarIn, zipOut);
                }
                zipOut.closeArchiveEntry();
            }
        }
        outputFiles.add(zipFile);
        return outputFiles;
    }
}
