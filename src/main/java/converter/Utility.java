package converter;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Utility {

    private static final Logger logger = LogManager.getLogger(Utility.class);

    /**
     * Copia il contenuto da un InputStream a un OutputStream
     */
    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Crea un file ZIP contenente le immagini dell'elenco, salvate in formato PNG.
     *
     * @param images      Lista di immagini BufferedImage
     * @param outputZip   File ZIP di destinazione
     * @throws IOException in caso di errore di scrittura
     */

    /**
     * Crea un file ZIP contenente tutti i file specificati.
     *
     * @param files Lista di file da includere
     * @return File ZIP generato
     * @throws IOException in caso di errore di lettura/scrittura
     */
    public static File zipper(List<File> files) throws IOException {
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
    public static List<File> unzipper(File zipFile) throws IOException {
        List<File> extractedFiles = new ArrayList<>();
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
     * Elimina ricorsivamente una directory e tutto il suo contenuto.
     *
     * @param dir La directory da eliminare
     * @return true se la directory è stata eliminata con successo, false altrimenti
     */
    public static boolean deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return false;

        File[] contents = dir.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        return dir.delete();
    }
}
