package converter;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Utility {

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
     * Crea un file ZIP contenente tutti i file immagine specificati.
     *
     * @param imageFiles Lista di file immagine da includere (PNG, JPG, BMP, ecc.)
     * @return File ZIP generato
     * @throws IOException in caso di errore di lettura/scrittura
     */
    public static File zipImages(List<File> imageFiles) throws IOException {
        File outputZip = new File("src/temp/images.zip"); // nome zip più esplicito

        try (FileOutputStream fos = new FileOutputStream(outputZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File imageFile : imageFiles) {
                if (!imageFile.exists() || !imageFile.isFile()) {
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

}
