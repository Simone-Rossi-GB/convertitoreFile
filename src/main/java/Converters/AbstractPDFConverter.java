package Converters;

import org.apache.pdfbox.pdmodel.PDDocument;
import java.io.File;
import java.util.ArrayList;

public abstract class AbstractPDFConverter implements Converter {

    @Override
    public ArrayList<File> convert(File srcFile) throws Exception {
        // Tenta prima senza password
        try {
            return convertProtectedFile(srcFile, null);
        } catch (Exception e) {
            // Se fallisce e siamo nell'ambiente GUI, chiedi la password
            if (isGuiEnvironment()) {
                String password = gui.MainViewController.launchDialogStringParameter();
                return convertProtectedFile(srcFile, password);
            } else {
                // Se siamo nel web service, rilancia l'eccezione
                throw new PasswordRequiredException("Il file PDF richiede una password: " + e.getMessage());
            }
        }
    }

    @Override
    public ArrayList<File> convert(File srcFile, String password) throws Exception {
        return convertProtectedFile(srcFile, password);
    }

    @Override
    public ArrayList<File> convert(File srcFile, boolean option) throws Exception {
        return convertProtectedFile(srcFile, null, option);
    }

    @Override
    public ArrayList<File> convert(File srcFile, String password, boolean option) throws Exception {
        return convertProtectedFile(srcFile, password, option);
    }

    @Override
    public boolean requiresPassword(File srcFile) {
        try {
            // Tenta di caricare il PDF senza password
            PDDocument pdf = PDDocument.load(srcFile);
            pdf.close();
            return false; // Se riesce, non richiede password
        } catch (Exception e) {
            // Se fallisce, probabilmente richiede password
            return true;
        }
    }

    @Override
    public boolean canConvertWithoutParameters(File srcFile) {
        return !requiresPassword(srcFile);
    }

    @Override
    public boolean supportsBooleanOption(File srcFile) {
        // Per i PDF che si convertono in JPG, supporta l'opzione di unione
        return true;
    }

    @Override
    public String getStringParameterDescription() {
        return "Password PDF";
    }

    @Override
    public String getBooleanOptionDescription() {
        return "Unire le pagine in un'unica immagine";
    }

    /**
     * Verifica se siamo nell'ambiente GUI o web service usando ConverterContext
     * @return true se siamo nell'ambiente GUI
     */
    private boolean isGuiEnvironment() {
        return ConverterContext.isGuiEnvironment();
    }

    /**
     * Conversione con password
     */
    public ArrayList<File> convertProtectedFile(File pdfFile, String password) throws Exception {
        return convertProtectedFile(pdfFile, password, false);
    }

    /**
     * Conversione con password e opzione booleana
     */
    public ArrayList<File> convertProtectedFile(File pdfFile, String password, boolean option) throws Exception {
        if (pdfFile == null) {
            throw new IllegalArgumentException("Il file PDF non può essere nullo");
        }

        PDDocument pdf = null;
        try {
            if (password == null || password.isEmpty()) {
                pdf = PDDocument.load(pdfFile);
            } else {
                pdf = PDDocument.load(pdfFile, password);
            }
        } catch (Exception e) {
            if (password == null || password.isEmpty()) {
                throw new PasswordRequiredException("File protetto da password: " + e.getMessage());
            } else {
                throw new Exception("Password errata: " + e.getMessage());
            }
        }

        try {
            return convertInternal(pdfFile, pdf, option);
        } catch (Exception e) {
            throw new Exception("Errore durante la conversione: " + e.getMessage());
        } finally {
            if (pdf != null) {
                pdf.close();
            }
        }
    }

    /**
     * Valida gli input
     */
    protected void validateInputs(File pdfFile, PDDocument pdfDocument) throws IllegalArgumentException {
        if (pdfFile == null) throw new IllegalArgumentException("L'oggetto pdfFile non esiste");
        if (pdfDocument == null) throw new IllegalArgumentException("L'oggetto pdfDocument non esiste");
    }

    /**
     * Metodo di conversione da implementare nelle sottoclassi
     */
    protected abstract ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument, boolean option) throws Exception;

    /**
     * Eccezione per file che richiedono password
     */
    public static class PasswordRequiredException extends Exception {
        public PasswordRequiredException(String message) {
            super(message);
        }
    }
}