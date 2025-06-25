package webService.configuration.jsonUtilities.recognisedWrappers;

/**
 * Interfaccia marker per rappresentare un tipo di input riconosciuto
 * (es. {@code String}, {@code File}, ecc.) usato nelle operazioni JSON.
 * <p>
 * Le classi che la implementano devono fornire accesso al valore grezzo
 * tramite il metodo {@code getValue()}, tipicamente utilizzato in fase di lettura/validazione.
 */
public interface RecognisedInput {

    /**
     * Restituisce il valore contenuto nell'input riconosciuto.
     * Il tipo restituito è generico per consentire flessibilità a livello di implementazione.
     *
     * @return valore non elaborato associato all’input
     */
    Object getValue();
}
