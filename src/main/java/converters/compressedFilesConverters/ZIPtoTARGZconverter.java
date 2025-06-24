package converters.compressedFilesConverters;

import converters.ConverterDocumentsWithPasword;
import converters.exception.FileMoveException;
import converters.exception.PasswordException;
import converter.Utility;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Questa classe si occupa della conversione di file .zip in .tar.gz,
 * con supporto per file ZIP protetti da password.
 */
public class ZIPtoTARGZconverter extends ConverterDocumentsWithPasword {

    private static final Logger logger = LogManager.getLogger(ZIPtoTARGZconverter.class);

    /**
     * Converte un file ZIP (protetto o no) in un archivio .tar.gz
     * @param zipFile Il file ZIP da convertire
     * @param password Password opzionale (null o vuota se non protetto)
     * @return Lista con il file .tar.gz risultante
     * @throws IOException In caso di problemi di lettura/scrittura
     * @throws FileMoveException In caso di impossibilità nel creare una directory temporanea
     * @throws PasswordException In caso di password errata o mancante
     */
    @Override
    public File convertProtectedFile(File zipFile, String password) throws FileMoveException, PasswordException, IOException {

        logger.info("Conversione iniziata con parametri:\n | zipFile.getPath() = {}", zipFile.getAbsolutePath());
        File outputTarGz = null;

        // Crea directory temporanea per estrazione
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "unzip_temp_" + System.nanoTime());
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            logger.error("Impossibile creare la directory temporanea: {}", tempDir.getAbsolutePath());
            throw new FileMoveException("Impossibile creare la directory temporanea.");
        }

        try {
            // Usa zip4j per apertura e estrazione ZIP
            ZipFile zip = new ZipFile(zipFile);

            //Controlla se il file è protetto
            if (zip.isEncrypted()) {
                if (password != null && !password.trim().isEmpty()) {
                    zip.setPassword(password.toCharArray());
                    logger.info("ZIP protetto: password fornita.");
                } else {
                    logger.error("ZIP protetto ma password non fornita.");
                    throw new PasswordException("Password mancante per file ZIP protetto.");
                }
            } else {
                logger.info("ZIP non protetto da password.");
            }

            // Estrazione nel temporaneo
            zip.extractAll(tempDir.getAbsolutePath());

            // Prepara file output tar.gz
            String baseName = zipFile.getName().replaceFirst("\\.zip$", "");
            outputTarGz = new File("src/temp", baseName + ".tar.gz");

            try (FileOutputStream fos = new FileOutputStream(outputTarGz);
                 GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(fos);
                 TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gcos)) {

                tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

                // Aggiunge ricorsivamente i file estratti al tar.gz
                addFilesToTarGz(tarOut, tempDir, "");

                logger.info("Creazione file .tar.gz completata: {}", outputTarGz.getAbsolutePath());
            } catch (IOException e) {
                logger.error("Problema durante la creazione del file .tar.gz: {}", e.getMessage());
                throw new IOException("Problema durante la creazione del file .tar.gz: " + e.getMessage());
            }

        } catch (ZipException e) {
            logger.error("Password errata");
            throw new PasswordException("Password errata");
        } finally {
            // Pulisce la directory temporanea
            if (!Utility.deleteDirectory(tempDir)) {
                logger.warn("Non è stato possibile cancellare la directory temporanea: {}", tempDir.getAbsolutePath());
            }
        }

        return outputTarGz;
    }

    /**
     * Aggiunge ricorsivamente file e directory all'archivio tar.gz
     * @param tarOut stream tar.gz di destinazione
     * @param source file o directory da aggiungere
     * @param basePath percorso relativo dentro il tar.gz
     * @throws IOException in caso di errori di I/O
     */
    private void addFilesToTarGz(TarArchiveOutputStream tarOut, File source, String basePath) throws IOException {
        String entryName = basePath + source.getName();
        //Controlla ricorsivamente le cartelle contenute
        if (source.isDirectory()) {
            File[] files = source.listFiles();
            if (files != null) {
                for (File file : files) {
                    addFilesToTarGz(tarOut, file, entryName + "/");
                }
            }
        } else {
            //Crea una nuova entry
            TarArchiveEntry entry = new TarArchiveEntry(source, entryName);
            entry.setSize(source.length());
            //Aggiunge l'entry allo stream
            tarOut.putArchiveEntry(entry);
            // Copia il contenuto del file nello stream tar.gz
            try (FileInputStream fis = new FileInputStream(source)) {
                Utility.copy(fis, tarOut);
            }
            tarOut.closeArchiveEntry();
        }
    }
}
