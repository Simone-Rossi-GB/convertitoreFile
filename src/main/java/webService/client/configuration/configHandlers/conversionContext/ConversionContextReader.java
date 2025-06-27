package webService.client.configuration.configHandlers.conversionContext;

/**
 * Fornisce metodi di accesso ai dati presenti nel contesto di conversione corrente per il thread.
 * <p>
 * Estende {@link ConversionContextData} e consente di leggere parametri rilevanti
 * utilizzati durante i processi di conversione, come formato di destinazione o opzioni di output.
 */
public class ConversionContextReader extends ConversionContextData {

    /**
     * Restituisce il formato di destinazione per la conversione.
     *
     * @return estensione o nome del formato (es. "pdf", "jpeg")
     */
    public static String getDestinationFormat() {
        return context.get().get("destinationFormat").toString();
    }

    /**
     * Restituisce la password da utilizzare per operazioni protette (es. compressione cifrata).
     *
     * @return stringa contenente la password
     */
    public static String getPassword() {
        return context.get().get("password").toString();
    }

    /**
     * Indica se i file devono essere uniti in un’unica entità di output.
     *
     * @return {@code true} se è richiesta l’unione; {@code false} altrimenti
     */
    public static boolean getIsUnion() {
        return (boolean) context.get().get("union");
    }

    /**
     * Indica se l'output della conversione deve essere compresso in un archivio ZIP.
     *
     * @return {@code true} se l’output va zippato; {@code false} in caso contrario
     */
    public static boolean getIsZippedOutput() {
        return (boolean) context.get().get("zippedOutput");
    }

    /**
     * Indica se i file convertiti devono essere protetti da password quando possibile.
     *
     * @return {@code true} se è richiesta la protezione; {@code false} altrimenti
     */
    public static boolean getProtected() {
        return (boolean) context.get().get("protected");
    }

    /**
     * Restituisce il watermark da applicare
     *
     * @return stringa contenente il watermark
     */
    public static String getWatermark() {return context.get().get("watermark").toString();}
}
