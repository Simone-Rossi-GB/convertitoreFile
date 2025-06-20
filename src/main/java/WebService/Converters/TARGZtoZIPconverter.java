package WebService.Converters;

import converter.Log;
import converter.Utility;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;

/**
 * Convertitore che trasforma un archivio .tar.gz in un file .zip
 */
public class TARGZtoZIPconverter implements Converter {

    private static final Logger logger = LogManager.getLogger(TARGZtoZIPconverter.class);

    /**
     * Converte un file tar.gz in un archivio zip
     * @param tarGzFile File di input .tar.gz
     * @return Lista contenente il file zip generato
     * @throws IOException In caso di errore durante la lettura o scrittura
     */
    @Override
    public ArrayList<File> convert(File tarGzFile) throws IOException {
        ArrayList<File> outputFiles = new ArrayList<>();

        logger.info("Inizio conversione con parametri: \n | tarGz.getPath() = {}", tarGzFile.getPath());
        Log.addMessage("Inizio conversione tarGz: " + tarGzFile.getName() + " -> .zip");

        String directoryPath = "src/temp/";
        String name = tarGzFile.getName();
        String baseName = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        File zipFile = new File(directoryPath, baseName + ".zip");

        try (
                FileInputStream fis = new FileInputStream(tarGzFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                GzipCompressorInputStream gis = new GzipCompressorInputStream(bis);
                TarArchiveInputStream tarIn = new TarArchiveInputStream(gis);
                FileOutputStream fos = new FileOutputStream(zipFile);
                ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(fos)
        ) {
            TarArchiveEntry entry;

            while ((entry = tarIn.getNextTarEntry()) != null) {
                ZipArchiveEntry zipEntry = new ZipArchiveEntry(entry.getName());
                zipOut.putArchiveEntry(zipEntry);

                if (!entry.isDirectory()) {
                    try {
                        Utility.copy(tarIn, zipOut);
                    } catch (IOException e) {
                        logger.error("Impossibile copiare l'entry {} nell'archivio zip: {}", entry.getName(), e.getMessage(), e);
                        Log.addMessage("ERRORE: impossibile copiare l'entry " + entry.getName() + " nell'archivio zip.");
                        throw e;
                    }
                }

                zipOut.closeArchiveEntry();
            }

            logger.info("Creazione file .zip completata: {}", zipFile.getName());
            Log.addMessage("Creazione file .zip completata: " + zipFile.getName());
            outputFiles.add(zipFile);

        } catch (IOException e) {
            logger.error("Errore durante la conversione del file tar.gz: {}", e.getMessage(), e);
            Log.addMessage("ERRORE: problema durante la conversione del file tar.gz.");
            throw e;
        }

        return outputFiles;
    }
}
