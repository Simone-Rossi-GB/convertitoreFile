package converter;

import java.io.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class Utility {

    /**
     * Restituisce l'estensione del file.
     *
     * @param file file da cui estrarre l'estensione
     * @return estensione in minuscolo o stringa vuota se non presente
     */
    public static String getExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length() - 1) {
            return ""; // nessuna estensione
        }
        return name.substring(lastDot + 1).toLowerCase();
    }

    public static String getBaseName(File file){
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1 || lastDot == name.length() - 1) {
            return ""; // nessun basename
        }
        return name.substring(0, lastDot);
    }

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
