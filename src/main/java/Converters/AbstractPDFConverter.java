package Converters;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.util.ArrayList;

public abstract class AbstractPDFConverter implements Converter{
    /**
     * Conversione base
     * @param pdfFile File di partenza
     * @return ArrayList di file convertiti
     * @throws Exception Pdf null o errori nel caricamento del file
     */
    @Override
    public ArrayList<File> convert(File pdfFile) throws Exception {
        return convertWithPassword(pdfFile, null, null);
    }

    /**
     * Conversione pdf -> immagine
     * @param pdfFile File di partenza
     * @param union Boolean per decidere se unire o no le pagine del pdf in un'unica immagine
     * @retur@param union Boolean per decidere se unire o no le pagine del pdf in un'unica immaginern ArrayList di file convertiti
     * @throws Exception Pdf null o errori nel caricamento del file
     */
    @Override
    public ArrayList<File> convert(File pdfFile, boolean union) throws Exception {
        return convertWithPassword(pdfFile, null, union);
    }

    /**
     * Conversione pdf protetto
     * @param pdfFile File di partenza
     * @param password Password inserita dall'utente per accedere al PDF
     * @return ArrayList di file convertiti
     * @throws Exception Pdf null o errori nel caricamento del file
     */
    @Override
    public ArrayList<File> convert(File pdfFile, String password) throws Exception {
        return convertWithPassword(pdfFile, password, null);
    }

    /**
     * Conversione pdf protetto -> immagine
     * @param pdfFile File di partenza
     * @param password Password inserita dall'utente per accedere al PDF
     * @param union Boolean per decidere se unire o no le pagine del pdf in un'unica immagine
     * @return ArrayList di file convertiti
     * @throws Exception Pdf null o errori nel caricamento del file
     */
    @Override
    public ArrayList<File> convert(File pdfFile, String password, boolean union) throws Exception {
        return convertWithPassword(pdfFile, password, union);
    }

    /**
     *Prova a caricare il pdf con la password passata (se non null) e seleziona il metodo di conversione coerente con i parametri passati
     * @param pdfFile File di partenza
     * @param password Password inserita dall'utente per accedere al PDF (può essere null)
     * @param union Boolean per decidere se unire o no le pagine del pdf in un'unica immagine (può essere null)
     * @return ArrayList di file convertiti
     * @throws Exception Pdf null o errori nel caricamento del file
     */
    private ArrayList<File> convertWithPassword(File pdfFile, String password, Boolean union) throws Exception {
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
                throw new Exception("File protetto da password: " + e.getMessage());
            } else {
                throw new Exception("Password errata");
            }
        }

        try{
            if(union == null)
                return convertInternal(pdfFile, pdf);
            else
                return convertInternal(pdfFile, pdf, union);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        } finally {
            if (pdf != null) {
                pdf.close();
            }
        }
    }

    /**
     * Metodo per controllare che i parametri non siano null
     * @param pdfFile File di partenza
     * @param pdfDocument Documento pdf caricato
     * @throws IllegalArgumentException Parametro null
     */
    protected void validateInputs(File pdfFile, PDDocument pdfDocument) throws IllegalArgumentException {
        if (pdfFile == null) throw new IllegalArgumentException("L'oggetto pdfFile non esiste");
        if (pdfDocument == null) throw new IllegalArgumentException("L'oggetto pdfDocument non esiste");
    }

    /**
     * Metodo conversione da pdf a doc/docx da implementare nelle sottoclassi
     * @param pdfFile File di partenza
     * @param pdfDocument Documento pdf caricato
     * @return ArrayList di file convertiti
     * @throws Exception
     */
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument) throws Exception{return null;}

    // Metodo astratto da implementare nelle sottoclassi
    protected ArrayList<File> convertInternal(File pdfFile, PDDocument pdfDocument, boolean union) throws Exception{return null;}


}
