package Converters;

import converter.Log;
import converter.Utility;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Questa classe si occupa della conversione di file .zip in .tar.gz
 */
public class ZIPtoTARGZconverter implements Converter {
    private static final Logger logger = LogManager.getLogger(ZIPtoTARGZconverter.class);
    /**
     * Converte un file ZIP in un archivio TAR.GZ
     * @param zipFile Il file ZIP da convertire
     * @return Lista con il file .tar.gz risultante
     * @throws IOException In caso di errori durante la lettura/scrittura
     */
    @Override
    public ArrayList<File> convert(File zipFile) throws IOException {
        ArrayList<File> outputFiles = new ArrayList<>();

        logger.info("Conversione iniziata con parametri:\n | zipFile.getPath() = {}", zipFile.getPath());
        // Preparazione percorso di output
        String directoryPath = "src/temp/";
        String zipName = zipFile.getName();
        String baseName = zipName.contains(".") ? zipName.substring(0, zipName.lastIndexOf('.')) : zipName;
        File tarGzOut = new File(directoryPath, baseName + ".tar.gz");

        // Apertura delle risorse con try-with-resources
        try (ZipFile zip = new ZipFile(zipFile);
             FileOutputStream fos = new FileOutputStream(tarGzOut);
             GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gcos)) {

            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            Enumeration<ZipArchiveEntry> entries = zip.getEntries();

            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();

                // Solo i file, non le directory
                if (!entry.isDirectory()) {
                    try (InputStream entryInputStream = zip.getInputStream(entry)) {
                        TarArchiveEntry tarEntry = new TarArchiveEntry(entry.getName());
                        tarEntry.setSize(entry.getSize());
                        tarOut.putArchiveEntry(tarEntry);
                        Utility.copy(entryInputStream, tarOut);
                        tarOut.closeArchiveEntry();
                    } catch (IOException e) {
                        logger.error("ERRORE: Impossibile copiare l'entry {} nel tar.", entry.getName());
                        throw e;
                    }
                }
            }
            logger.info("Creazione file .tar.gz completata: {}", tarGzOut.getName());
            outputFiles.add(tarGzOut);

        } catch (IOException e) {
            logger.error("ERRORE: Problema durante la conversione del file ZIP.");
            throw e;
        }

        return outputFiles;
    }
}

