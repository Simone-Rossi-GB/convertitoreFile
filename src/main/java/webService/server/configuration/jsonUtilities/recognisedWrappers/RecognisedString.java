package webService.server.configuration.jsonUtilities.recognisedWrappers;

import webService.client.configuration.jsonUtilities.recognisedWrappers.RecognisedInput;

/**
 * Wrapper per rappresentare una {@link String} come input riconosciuto
 * nelle operazioni di parsing, validazione o scrittura di contenuti JSON.
 * <p>
 * Implementa {@link webService.client.configuration.jsonUtilities.recognisedWrappers.RecognisedInput} per fornire accesso tipizzato al valore grezzo
 * e favorire polimorfismo in metodi generici che trattano diversi tipi di input.
 */
public class RecognisedString implements RecognisedInput {

    /** Stringa testuale da incapsulare. */
    private final String value;

    /**
     * Costruttore che inizializza il wrapper con una stringa specifica.
     *
     * @param value contenuto testuale da rappresentare
     */
    public RecognisedString(String value) {
        this.value = value;
    }

    /**
     * Restituisce il valore stringa contenuto in questo wrapper.
     *
     * @return stringa grezza rappresentata da questo input
     */
    @Override
    public String getValue() {
        return value;
    }
}
