package Converters;

import java.io.File;
import java.util.ArrayList;

public interface Converter {
    /**
     * Conversione base senza parametri aggiuntivi
     */
    ArrayList<File> convert(File srcFile) throws Exception;

    /**
     * Conversione con parametri aggiuntivi (password, opzioni, ecc.)
     */
    default ArrayList<File> convert(File srcFile, String parameter) throws Exception {
        return convert(srcFile);
    }

    /**
     * Conversione con parametro booleano (es. unione immagini)
     */
    default ArrayList<File> convert(File srcFile, boolean option) throws Exception {
        return convert(srcFile);
    }

    /**
     * Conversione con entrambi i parametri
     */
    default ArrayList<File> convert(File srcFile, String parameter, boolean option) throws Exception {
        return convert(srcFile, parameter);
    }

    /**
     * Verifica se il file richiede parametri aggiuntivi
     * @param srcFile file da controllare
     * @return true se il file può essere convertito senza parametri aggiuntivi
     */
    default boolean canConvertWithoutParameters(File srcFile) {
        return true;
    }

    /**
     * Verifica se il converter richiede una password
     * @param srcFile file da controllare
     * @return true se il file richiede una password
     */
    default boolean requiresPassword(File srcFile) {
        return false;
    }

    /**
     * Verifica se il converter supporta opzioni booleane (es. unione immagini)
     * @param srcFile file da controllare
     * @return true se il converter supporta opzioni booleane
     */
    default boolean supportsBooleanOption(File srcFile) {
        return false;
    }

    /**
     * Ottiene una descrizione del parametro stringa richiesto
     * @return descrizione del parametro (es. "Password PDF")
     */
    default String getStringParameterDescription() {
        return "Parametro";
    }

    /**
     * Ottiene una descrizione dell'opzione booleana
     * @return descrizione dell'opzione (es. "Unire le pagine in un'unica immagine")
     */
    default String getBooleanOptionDescription() {
        return "Opzione";
    }
}