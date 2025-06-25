package webService;

import webService.server.converters.exception.IllegalExtensionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Utility {

    /**
     * Restituisce l'estensione del file.
     *
     * @param file file da cui estrarre l'estensione
     * @return estensione in minuscolo o stringa vuota se non presente
     */
    public static String getExtension(File file) throws IllegalExtensionException{
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        //Se non viene trovato un punto o si trova come ultimo carattere lancia un'eccezione
        if (lastDot == -1 || lastDot == name.length() - 1) {
            throw new IllegalExtensionException("Il file non ha un'estensoine");
        }
        return name.substring(lastDot + 1).toLowerCase();
    }

    /**
     * Restituisce il nome del file senza estensione
     * @param file file di partenza
     * @return nome del file senza estensione e in maniera case-sensitive
     */
    public static String getBaseName(File file){
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        //Se non viene trovato un punto o si trova come ultimo carattere ritorna direttamente il nome
        if (lastDot == -1 || lastDot == name.length() - 1) {
            return name;
        }
        return name.substring(0, lastDot);
    }

    private static final Logger logger = LogManager.getLogger(Utility.class);


}
