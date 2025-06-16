package Converters;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import java.io.*;
import java.util.Enumeration;


public class ACC {
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int n;
        while((n = in.read(buffer)) != - 1){
            out.write(buffer, 0, n);
        }
    }

    public static void convertiZipToTarGz(File zipFile, File tarGzOut) throws IOException{
        ZipFile zip = null;
        TarArchiveOutputStream tarOut = null;

        try{
            zip = new ZipFile(zipFile);
            FileOutputStream fos = new FileOutputStream(tarGzOut);
            GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
            tarOut = new TarArchiveOutputStream(gcos);
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            Enumeration<ZipArchiveEntry> entrate = zip.getEntries();
            while (entrate.hasMoreElements()){
                ZipArchiveEntry entrata = entrate.nextElement();

                if (!entrata.isDirectory()) {
                    InputStream input = zip.getInputStream(entrata);
                    TarArchiveEntry tarEntry = new TarArchiveEntry(entrata.getName());
                    tarEntry.setSize(entrata.getSize());
                    tarOut.putArchiveEntry(tarEntry);
                    copy(input, tarOut);
                    tarOut.closeArchiveEntry();
                    input.close();
                }
            }
        } finally {
            if (zip != null) zip.close();
            if (tarOut != null) tarOut.close();
        }
    }

    public static void convertiZipToTarGzInCartella(File zipFile, File outputFolder) throws IOException {
        String zipName = zipFile.getName();
        int lastDot = zipName.lastIndexOf('.');
        String baseName = (lastDot == -1) ? zipName : zipName.substring(0, lastDot);

        File tarGzFile = new File(outputFolder, baseName + ".tar.gz");

        convertiZipToTarGz(zipFile, tarGzFile);
    }

    private static void convertiTarGzToZip(File tarGzFile, File zipFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(tarGzFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gis);
             FileOutputStream fos = new FileOutputStream(zipFile);
             ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(fos)) {

            TarArchiveEntry entrata;
            while ((entrata = tarIn.getNextTarEntry()) != null) {
                String nomeEntrtata = entrata.getName();

                ZipArchiveEntry zipEntry = new ZipArchiveEntry(nomeEntrtata);
                zipOut.putArchiveEntry(zipEntry);
                if (entrata.isDirectory()) {
                    // Per le directory, non ci sono dati da copiare ma dobbiamo comunque creare l'entry nel zip.
                    zipOut.closeArchiveEntry();
                } else {
                    // Per i file, copiamo i dati dal tar al zip
                    copy(tarIn, zipOut);
                    zipOut.closeArchiveEntry();
                }
            }
        }
    }

    public static void convertiTarGzToZipInCartella(File tarGzFile, File outputFolder) throws IOException {
        String tarGzName = tarGzFile.getName();
        int lastDot = tarGzName.lastIndexOf('.');
        String baseName = (lastDot == -1) ? tarGzName : tarGzName.substring(0, lastDot);

        File zipFile = new File(outputFolder, baseName + ".zip");

        convertiTarGzToZip(tarGzFile, zipFile);
    }
}

