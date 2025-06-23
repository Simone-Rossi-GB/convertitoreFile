package configuration.jsonUtilities.recognisedWrappers;

import java.io.File;

/**
 * Wrapper per rappresentare un {@link File} come input riconosciuto
 * nelle operazioni di validazione o parsing JSON.
 * <p>
 * Implementa l'interfaccia {@link RecognisedInput} per consentire
 * l’accesso sicuro e tipizzato al valore sottostante.
 */
public class RecognisedFile implements RecognisedInput {

    /** File di configurazione incapsulato. */
    private final File file;

    /**
     * Costruttore che inizializza il wrapper con un file specifico.
     *
     * @param file il file JSON da elaborare
     */
    public RecognisedFile(File file) {
        this.file = file;
    }

    /**
     * Restituisce il file incapsulato in questo input riconosciuto.
     *
     * @return oggetto {@code File} contenuto
     */
    @Override
    public File getValue() {
        return file;
    }
}
