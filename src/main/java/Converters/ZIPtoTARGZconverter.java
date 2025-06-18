package Converters;

import converter.Log;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;

public class ZIPtoTARGZconverter implements Converter{
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int n;
        while ((n = in.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
    }

    @Override
    public ArrayList<File> convert(File zipFile) throws IOException {
        ArrayList<File> outputFiles = new ArrayList<>();
        Log.addMessage("Inizio conversione csv: "+ zipFile.getName() + " -> .json");
        String directoryPath = "src/temp/";
        String zipName = zipFile.getName();
        int lastDot = zipName.lastIndexOf('.');
        String baseName = (lastDot == -1) ? zipName : zipName.substring(0, lastDot);
        File tarGzOut = new File(directoryPath, baseName + ".tar.gz");

        try (ZipFile zip = new ZipFile(zipFile);
             FileOutputStream fos = new FileOutputStream(tarGzOut);
             GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gcos)) {

            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();

            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    InputStream input = zip.getInputStream(entry);
                    TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getName());
                    tarEntry.setSize(entry.getSize());
                    tarOut.putArchiveEntry(tarEntry);
                    copy(input, tarOut);
                    tarOut.closeArchiveEntry();
                    input.close();
                }
            }
        }

        outputFiles.add(tarGzOut);
        Log.addMessage("Creazione file .json completata: " + tarGzOut.getName());
        return outputFiles;
    }
}

