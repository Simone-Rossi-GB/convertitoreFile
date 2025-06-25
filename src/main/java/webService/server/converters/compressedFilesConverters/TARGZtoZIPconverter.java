package webService.server.converters.compressedFilesConverters;

import webService.client.objects.Utility;
import webService.server.converters.Converter;
import webService.server.converters.exception.ConversionException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Convertitore che trasforma un archivio .tar.gz in un file .zip
 */
public class TARGZtoZIPconverter extends Converter {

    private static final Logger logger = LogManager.getLogger(TARGZtoZIPconverter.class);

    /**
     * Converte un file tar.gz in un archivio zip
     * @param tarGzFile File di input .tar.gz
     * @return Lista contenente il file zip generato
     * @throws IOException In caso di errore durante la lettura o scrittura
     */
    @Override
    public File convert(File tarGzFile) throws IOException {
        logger.info("Inizio conversione con parametri: \n | tarGz.getPath() = {}", tarGzFile.getPath());

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
                        throw e;
                    }
                }

                zipOut.closeArchiveEntry();
            }

            logger.info("Creazione file .zip completata: {}", zipFile.getName());

        } catch (IOException e) {
            logger.error("Errore durante la conversione del file tar.gz: {}", e.getMessage(), e);
            throw new ConversionException("Errore durante la conversione del file tar.gz");
        }

        return zipFile;
    }
}
