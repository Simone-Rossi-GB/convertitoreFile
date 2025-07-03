package webService.server.config.configHandlers.conversionContext;

import webService.server.config.configExceptions.JsonStructureException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashMap;

/**
 * Fornisce metodi di accesso ai dati presenti nel contesto di conversione corrente per il thread.
 * <p>
 * Estende {@link ConversionContextData} e consente di leggere parametri rilevanti
 * utilizzati durante i processi di conversione, come formato di destinazione o opzioni di output.
 */
public class ConversionContextReader extends ConversionContextData {
    private static final Logger logger = LogManager.getLogger(ConversionContextReader.class);

    /**
     * Ottiene un valore dal context con gestione errori robusta.
     */
    private static Object getContextValue(String key, Object defaultValue) {
        try {
            HashMap<String, Object> currentContext = getCurrentContext();
            if (currentContext == null) {
                logger.error("Context è null per thread: {}", Thread.currentThread().getName());
                return defaultValue;
            }

            if (!currentContext.containsKey(key)) {
                logger.error("Chiave '{}' non trovata nel context. Chiavi disponibili: {}", key, currentContext.keySet());
                return defaultValue;
            }

            Object value = currentContext.get(key);
            logger.debug("Valore per chiave '{}': {}", key, value);
            return value != null ? value : defaultValue;

        } catch (Exception e) {
            logger.error("Errore accesso context per chiave '{}': ", key, e);
            return defaultValue;
        }
    }

    /**
     * Restituisce il formato di destinazione per la conversione.
     */
    public static String getDestinationFormat() {
        Object value = getContextValue("destinationFormat", "pdf");
        String result = value.toString();
        logger.info("getDestinationFormat() = {}", result);
        return result;
    }

    /**
     * Restituisce la password da utilizzare per operazioni protette.
     */
    public static String getPassword() {
        Object value = getContextValue("password", "");
        return value.toString();
    }

    /**
     * Indica se i file devono essere uniti in un'unica entità di output.
     */
    public static boolean getIsUnion() {
        Object value = getContextValue("union", false);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        // Fallback per string boolean
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Indica se l'output della conversione deve essere compresso in un archivio ZIP.
     */
    public static boolean getIsZippedOutput() {
        Object value = getContextValue("zippedOutput", false);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Indica se i file convertiti devono essere protetti da password quando possibile.
     */
    public static boolean getProtected() {
        Object value = getContextValue("protected", false);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Restituisce il watermark da applicare
     */
    public static String getWatermark() {
        Object value = getContextValue("watermark", "");
        return value.toString();
    }

    /**
     * Indica se la conversione multipla è abilitata.
     */
    public static Boolean getIsMultipleConversionEnabled() throws JsonStructureException {
        Object value = getContextValue("multipleConversion", false);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Metodo di debug per verificare stato del context
     */
    public static void debugContext() {
        logger.info("=== DEBUG CONTEXT ===");
        logger.info("Thread: {}", Thread.currentThread().getName());
        logger.info("JsonFile: {}", getJsonFile() != null ? getJsonFile().getAbsolutePath() : "null");

        HashMap<String, Object> currentContext = getCurrentContext();
        if (currentContext != null) {
            logger.info("Context size: {}", currentContext.size());
            logger.info("Context keys: {}", currentContext.keySet());
            currentContext.forEach((k, v) -> logger.info("  {} = {}", k, v));
        } else {
            logger.error("Context è NULL!");
        }
        logger.info("=== END DEBUG ===");
    }
}