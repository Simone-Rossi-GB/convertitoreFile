package Converters;

import Converters.exception.IllegalExtensionException;
import converter.Log;
import converter.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Zipper {
    private static final Logger logger = LogManager.getLogger(Zipper.class);

    /**
     * Crea un file ZIP contenente tutti i file specificati.
     *
     * @param files Lista di file da includere
     * @return File ZIP generato
     * @throws IOException in caso di errore di lettura/scrittura
     */
    public static File zip(List<File> files) throws IOException {
        File outputZip = new File("src/temp/images.zip"); // nome zip più esplicito

        try (FileOutputStream fos = new FileOutputStream(outputZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File imageFile : files) {
                if (!imageFile.exists() || !imageFile.isFile()) {
                    logger.warn("File non valido: {}", imageFile.getAbsolutePath());
                    Log.addMessage("File non valido: " + imageFile.getAbsolutePath());
                    continue;
                }

                try (FileInputStream fis = new FileInputStream(imageFile)) {
                    // Crea un entry con il nome del file originale
                    zos.putNextEntry(new ZipEntry(imageFile.getName()));

                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }

                    zos.closeEntry();
                }
            }
        }
        return outputZip;
    }
    public static ArrayList<File> unzip(File zipFile) throws IOException {
        ArrayList<File> extractedFiles = new ArrayList<>();
        File outputDir = new File("src/temp/");

        // Crea la cartella di output se non esiste
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(outputDir, entry.getName());

                // Crea sottocartelle se necessario
                if (entry.isDirectory()) {
                    outFile.mkdirs();
                    continue;
                } else {
                    File parent = outFile.getParentFile();
                    if (!parent.exists()) parent.mkdirs();
                }

                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    extractedFiles.add(outFile);
                }
                zis.closeEntry();
            }
        }

        return extractedFiles;
    }

    /**
     * Comprime una lista di file in un file zip
     * @param files ArrayList di file
     * @param baseName Nome del file.zip
     * @return File.zip
     * @throws IOException Impossibile rinominare il file
     */
    public static File compressioneFile(ArrayList<File> files, String baseName) throws IOException {
        logger.info("Compressione dei file generati in output");
        File zippedImages = zip(files);
        zippedImages = rinominaFileZip(zippedImages, baseName);
        logger.info("Compressione completata");
        return zippedImages;
    }

    /**
     * Comprime n file in un file zip
     * @param file file singolo da zippare
     * @param baseName Nome del file.zip
     * @return File.zip
     * @throws IOException Impossibile rinominare il file
     */
    public static File compressioneFile(File file, String baseName) throws IOException {
        ArrayList<File> files = new ArrayList<>();
        files.add(file);
        return compressioneFile(files, baseName);
    }

    /**
     * Rinomina il file.zip
     * @param zipFile file da rinominare
     * @param name nome da assegnare
     * @return File rinominato
     * @throws IOException Impossibile rinominare il file
     */
    private static File rinominaFileZip(File zipFile, String name) throws IOException {
        File renamedZip = new File(zipFile.getParent(), name + ".zip");
        try {
            Files.move(zipFile.toPath(), renamedZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return renamedZip;
        } catch (IOException e) {
            logger.error("Impossibile rinominare il file ZIP: {}", e.getMessage(), e);
            throw new IOException("Impossibile rinominare il file ZIP: " + e.getMessage(), e);
        }
    }

    /**
     * Ritorna le estensioni dei file contenuti nello zip se sono tutte uguali
     * @param zipFile file compresso
     * @return estensione comune
     * @throws IOException impossibile decomprimere lo zip
     * @throws IllegalExtensionException estensioni diverse
     */
    public static String extractFileExstension(File zipFile) throws IOException, IllegalExtensionException {
        List<File> files = unzip(zipFile);
        //Prende l'estensione del primo file
        String ext = Utility.getExtension(files.get(0));
        //Controlla se sono tutte uguali
        for(File f : files){
            if(!Utility.getExtension(f).equals(ext)){
                throw new IllegalExtensionException("I file non sono tutti dello stesso formato");
            }
        }
        return ext;
    }
}
