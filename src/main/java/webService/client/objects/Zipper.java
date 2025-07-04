package webService.client.objects;

import webService.client.configuration.configHandlers.conversionContext.ConversionContextReader;
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
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import webService.client.objects.exceptions.FileMoveException;
import webService.client.objects.exceptions.IllegalExtensionException;

public class Zipper {
    private static final Logger logger = LogManager.getLogger(Zipper.class);

    /**
     * Crea un file ZIP contenente tutti i file specificati.
     *
     * @param files Lista di file da includere
     * @return File ZIP generato
     * @throws IOException in caso di errore di lettura/scrittura
     */
    public static File zip(List<File> files) throws IOException, FileMoveException {
        File outputZip = new File(files.get(0).getParent(), "file.zip");

        try (FileOutputStream fos = new FileOutputStream(outputZip);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File f : files) {
                if (!f.exists() || !f.isFile()) {
                    logger.warn("File non valido: {}", f.getAbsolutePath());
                    continue;
                }
                //Legge il contenuto di ciascun file
                try (FileInputStream fis = new FileInputStream(f)) {
                    // Crea un entry con il nome del file originale
                    zos.putNextEntry(new ZipEntry(f.getName()));
                    // Scrive il contenuto del file nel file ZIP usando un buffer
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }

                    zos.closeEntry();
                }
                if (!f.delete()){
                    throw new FileMoveException("Impossibile eliminare il file temporaneo");
                }
            }
        }
        return outputZip;
    }


    /**
     * Crea un file ZIP contenente tutti i file specificati.
     *
     * @param files Lista di file da includere
     * @return File ZIP generato
     * @throws IOException in caso di errore di lettura/scrittura
     * @throws FileMoveException impossibile eliminare il file temporaneo
     */
    public static File zipWithPassword(List<File> files) throws IOException, FileMoveException {
        File outputZip = new File(files.get(0).getParent(), "file.zip");
        ZipFile zipFile = new ZipFile(outputZip, ConversionContextReader.getPassword().toCharArray());

        ZipParameters parameters = new ZipParameters();
        parameters.setEncryptFiles(true);
        parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

        for (File f : files) {
            if (!f.exists() || !f.isFile()) {
                logger.warn("File non valido: {}", f.getAbsolutePath());
                continue;
            }

            zipFile.addFile(f, parameters);

            if (!f.delete()) {
                throw new FileMoveException("Impossibile eliminare il file temporaneo");
            }
        }

        return outputZip;
    }


    /**
     * Decomprime un file zip in un arrayList di file
     * @param zipFile file da decomprimere
     * @return ArrayList di file contenuti nello zip
     * @throws IOException Errori nelle operazioni di lettura e scrittura
     */
    public static ArrayList<File> unzip(File zipFile) throws IOException {
        ArrayList<File> extractedFiles = new ArrayList<>();
        File outputDir = new File("src/temp");

        // Crea la cartella di output se non esiste
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            //Per ogni entry crea un file corrispondente
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
                //Scrive sul file creato il contenuto di quello compresso
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
    public static File compressioneFile(ArrayList<File> files, String baseName) throws IOException, FileMoveException {
        logger.info("Compressione dei file generati in output");
        //Crea il file zip
        File zippedFiles = zip(files);
        //Assegna al file il nome voluto
        zippedFiles = rinominaFileZip(zippedFiles, baseName);
        return zippedFiles;
    }

    /**
     * Comprime una lista di file in un file zip protetto
     * @param files ArrayList di file
     * @param baseName Nome del file.zip
     * @return File.zip
     * @throws IOException Impossibile rinominare il file
     */
    public static File compressioneFileProtetto(ArrayList<File> files, String baseName) throws IOException, FileMoveException {
        logger.info("Compressione dei file generati in output");
        //Crea il file zip
        File zippedFiles = zipWithPassword(files);
        //Assegna al file il nome voluto
        zippedFiles = rinominaFileZip(zippedFiles, baseName);
        return zippedFiles;
    }

    /**
     * Comprime un singolo file in un file zip protetto
     * @param file file singolo da zippare
     * @param baseName Nome del file.zip
     * @return File.zip
     * @throws IOException Impossibile rinominare il file
     */
    public static File compressioneFileProtetto(File file, String baseName) throws IOException, FileMoveException {
        //Crea una lista con un solo file
        ArrayList<File> files = new ArrayList<>();
        files.add(file);
        return compressioneFileProtetto(files, baseName);
    }

    /**
     * Comprime un singolo file in un file zip
     * @param file file singolo da zippare
     * @param baseName Nome del file.zip
     * @return File.zip
     * @throws IOException Impossibile rinominare il file
     */
    public static File compressioneFile(File file, String baseName) throws IOException, FileMoveException {
        //Crea una lista con un solo file
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
        //Crea un file con il nome desiderato ed estensione .zip nella stessa cartella
        File renamedZip = new File(zipFile.getParent(), name + ".zip");
        try {
            //sostituisce nel percorso indicato il file di partenza, che ora avrà il nome desiderato
            Files.move(zipFile.toPath(), renamedZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println(renamedZip.getName());
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
        logger.info("Estraggo le estensioni dei file compressi");
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
