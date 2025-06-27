package webService.client.configuration.configHandlers.conversionContext;

/**
 * Fornisce metodi per aggiornare i dati nel contesto di conversione
 * specifico per il thread corrente.
 * <p>
 * Estende {@link ConversionContextData} e consente di impostare valori utilizzati
 * durante il processo di conversione (es. formato di destinazione, opzioni avanzate...).
 */
public class ConversionContextWriter extends ConversionContextData {

    /**
     * Imposta il formato di destinazione per la conversione.
     *
     * @param newDestinationFormat nuovo formato da applicare (es. "pdf", "tiff", ...)
     */
    public static void setDestinationFormat(String newDestinationFormat) {
        context.get().put("destinationFormat", newDestinationFormat);
    }

    /**
     * Imposta una password da utilizzare durante la conversione, ad esempio per file ZIP protetti.
     *
     * @param newPassword password in chiaro
     */
    public static void setPassword(String newPassword) {
        context.get().put("password", newPassword);
    }

    /**
     * Specifica se i file devono essere uniti in un unico output (es. merge di più immagini/PDF).
     *
     * @param isUnion {@code true} per abilitare l’unione; {@code false} altrimenti
     */
    public static void setIsUnion(boolean isUnion) {
        context.get().put("union", isUnion);
    }

    /**
     * Specifica se l’output della conversione deve essere compresso in archivio ZIP.
     *
     * @param isZippedOutput {@code true} se l’output va zippato; {@code false} altrimenti
     */
    public static void setIsZippedOutput(boolean isZippedOutput) {
        context.get().put("zippedOutput", isZippedOutput);
    }

    /**
     * Specifica se l’output della conversione deve essere protetto da password quando possibile.
     *
     * @param isProtected {@code true} se l’output va protetto; {@code false} altrimenti
     */
    public static void setProtected(boolean isProtected) {
        context.get().put("protected", isProtected);
    }

    /**
     * Imposta un watermark
     *
     * @param newWatermark password in chiaro
     */
    public static void setWatermark(String newWatermark) {context.get().put("watermark", newWatermark);}

    /**
     * Abilita o disabilita la possibilità di effettuare conversioni multiple.
     *
     * @param isMultipleConversionEnabled valore booleano per "multipleConversion"
     */
    public void setIsMultipleConversionEnabled(boolean isMultipleConversionEnabled) {
        context.get().put("multipleConversion", isMultipleConversionEnabled);
    }
}
