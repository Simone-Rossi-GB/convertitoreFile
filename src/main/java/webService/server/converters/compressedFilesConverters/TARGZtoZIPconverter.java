package webService.server.converters.compressedFilesConverters;

import webService.server.Utility;
import webService.server.config.configHandlers.Config;
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
    public File convert(File tarGzFile, Config configuration) throws IOException {
        // Log dell'inizio del processo di conversione con il percorso del file in input
        logger.info("Inizio conversione con parametri: \n | tarGz.getPath() = {}", tarGzFile.getPath());

        // Estrae il nome base del file (senza estensione) per creare il nome del file zip
        String name = tarGzFile.getName();
        String baseName = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
        File zipFile = new File(tarGzFile.getParent(), baseName + ".zip");

        // Blocchi try-with-resources per gestire in sicurezza gli stream
        try (
                FileInputStream fis = new FileInputStream(tarGzFile); // Stream per leggere il file .tar.gz
                BufferedInputStream bis = new BufferedInputStream(fis); // Buffer per migliorare le prestazioni
                GzipCompressorInputStream gis = new GzipCompressorInputStream(bis); // Decompressione gzip
                TarArchiveInputStream tarIn = new TarArchiveInputStream(gis); // Lettura del contenuto dell'archivio tar
                FileOutputStream fos = new FileOutputStream(zipFile); // Stream per scrivere il file zip
                ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(fos) // Scrittura dell'archivio zip
        ) {
            TarArchiveEntry entry;

            // Ciclo per leggere ogni entry dell'archivio tar
            while ((entry = tarIn.getNextTarEntry()) != null) {
                ZipArchiveEntry zipEntry = new ZipArchiveEntry(entry.getName()); // Crea una nuova entry per lo zip
                zipOut.putArchiveEntry(zipEntry); // Aggiunge la entry allo zip

                if (!entry.isDirectory()) {
                    try {
                        // Copia i dati dalla entry tar alla entry zip
                        Utility.copy(tarIn, zipOut);
                    } catch (IOException e) {
                        // Log dell'errore e rilancio dell'eccezione se la copia fallisce
                        logger.error("Impossibile copiare l'entry {} nell'archivio zip: {}", entry.getName(), e.getMessage(), e);
                        throw e;
                    }
                }

                // Chiude la entry nello zip, indipendentemente se è file o cartella
                zipOut.closeArchiveEntry();
            }

            // Log al termine della creazione del file zip
            logger.info("Creazione file .zip completata: {}", zipFile.getName());

        } catch (IOException e) {
            // Log dell'errore e lancio di un'eccezione specifica
            logger.error("Errore durante la conversione del file tar.gz: {}", e.getMessage(), e);
            throw new ConversionException("Errore durante la conversione del file tar.gz");
        }

        // Ritorna il file zip creato
        return zipFile;
    }

}
