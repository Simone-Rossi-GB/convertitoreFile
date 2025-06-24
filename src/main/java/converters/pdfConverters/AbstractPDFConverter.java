package converters.pdfConverters;

import converters.ConverterDocumentsWithPasword;
import converters.exception.PasswordException;
import com.twelvemonkeys.util.convert.ConversionException;
import objects.DirectoryWatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.IOException;

public abstract class AbstractPDFConverter extends ConverterDocumentsWithPasword {
    private static final Logger logger = LogManager.getLogger(AbstractPDFConverter.class);
    /**
     *Prova a caricare il pdf con la password passata (se non null) e seleziona il metodo di conversione coerente con i parametri passati
     * @param pdfFile File di partenza
     * @param password Password letta dal JSON
     * @return ArrayList di file convertiti
     * @throws IllegalArgumentException Pdf null
     * @throws IOException Errori nel caricamento
     * @throws PasswordException Errori con la password
     */
    @Override
    public File convertProtectedFile(File pdfFile, String password) throws IllegalArgumentException, IOException, PasswordException {
        if (pdfFile == null) {
            logger.error("File PDF nullo");
            throw new IllegalArgumentException("Il file PDF non può essere nullo");
        }
        PDDocument pdf = null;
        //Prova a caricare il documento senza password
        try {
            pdf = PDDocument.load(pdfFile);
            logger.info("Documento PDF caricato con successo");
        }
        //Se è protetto prova con la password letta dal JSON
        catch (InvalidPasswordException e) {
            if (password == null || password.isEmpty())
                throw new PasswordException("Password richiesta");
            try{
                pdf = PDDocument.load(pdfFile, password);
                logger.info("Documento PDF caricato con successo");
            }catch (InvalidPasswordException ex) {
                throw new PasswordException("Password errata");
            }
        }catch (IOException e){
            throw new IOException("Errore nel caricamento del documento");
        }
        //Esegue la conversione
        try{
            return convertInternal(pdfFile, pdf);
        } catch (IOException e) {
            throw new ConversionException(e.getMessage());
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
     */
    protected abstract File convertInternal(File pdfFile, PDDocument pdfDocument) throws IOException;
}
