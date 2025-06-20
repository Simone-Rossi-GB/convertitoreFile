package Converters;

import gui.MainViewController;
import java.io.File;
import java.util.ArrayList;

public abstract class ConverterDocumentsStringParameter implements Converter {

    @Override
    public ArrayList<File> convert(File srcFile) throws Exception {
        // Per l'interfaccia grafica, lancia il dialogo
        String extraParameter = MainViewController.launchDialogStringParameter();
        return convertProtectedFile(srcFile, extraParameter);
    }

    public ArrayList<File> convert(File srcFile, String parameter) throws Exception {
        return convertProtectedFile(srcFile, parameter);
    }

    public boolean requiresPassword(File srcFile) {
        // Implementazione di default: controlla se può caricare il file senza parametri
        try {
            // Tenta la conversione senza parametri per vedere se fallisce
            canConvertWithoutParameters(srcFile);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean canConvertWithoutParameters(File srcFile) {
        try {
            // Tenta di verificare se il file richiede parametri
            // Questo metodo deve essere sovrascritto nelle sottoclassi per controlli specifici
            return !requiresPassword(srcFile);
        } catch (Exception e) {
            return false;
        }
    }

    public String getStringParameterDescription() {
        return "Password del documento";
    }

    /**
     * Metodo di conversione che deve essere implementato dalle sottoclassi
     * @param srcFile File sorgente
     * @param extraParameter Parametro extra (password, etc.)
     * @return Lista dei file convertiti
     * @throws Exception Se la conversione fallisce
     */
    public abstract ArrayList<File> convertProtectedFile(File srcFile, String extraParameter) throws Exception;
}