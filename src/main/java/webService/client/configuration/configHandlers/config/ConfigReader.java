package webService.client.configuration.configHandlers.config;

import webService.client.configuration.configExceptions.JsonStructureException;
import java.util.List;
import java.util.Map;

/**
 * Classe di utilità per accedere ai dati di configurazione letti dal file JSON.
 * <p>
 * Estende {@link ConfigData} e fornisce metodi statici per leggere valori specifici dalla mappa
 * {@code configDataMap}, che viene inizializzata tramite {@code ConfigData.update(...)}.
 */
public class ConfigReader extends ConfigData {

    /**
     * Restituisce l'elenco dei formati che supportano il canale alfa.
     *
     * @return lista di formati con canale alfa
     * @throws JsonStructureException se la mappa dei dati non è disponibile o malformata
     */
    @SuppressWarnings("unchecked")
    public static List<String> getFormatsWithAlphaChannel() throws JsonStructureException {
        return (List<String>) configDataMap.get("formatsWithAlphaChannel");
    }

    /**
     * Restituisce i formati che richiedono una conversione intermedia.
     *
     * @return lista di formati che richiedono una doppia conversione
     * @throws JsonStructureException se il valore è mancante o corrotto
     */
    @SuppressWarnings("unchecked")
    public static List<String> getFormatsRequiringIntermediateConversion() throws JsonStructureException {
        return (List<String>) configDataMap.get("formatsRequiringIntermediateConversion");
    }

    /**
     * Restituisce la struttura delle conversioni supportate.
     *
     * @return mappa con chiavi di formato sorgente, ognuna associata a un'altra mappa di formati destinazione
     * @throws JsonStructureException se il nodo è mancante o non mappabile
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, String>> getConversions() throws JsonStructureException {
        return (Map<String, Map<String, String>>) configDataMap.get("conversions");
    }

    /**
     * Restituisce la lista dei campi obbligatori per la struttura JSON.
     *
     * @return lista di chiavi richieste nel file di configurazione
     * @throws JsonStructureException se il campo non è presente
     */
    @SuppressWarnings("unchecked")
    public static List<String> getMandatoryEntries() throws JsonStructureException {
        return (List<String>) configDataMap.get("mandatoryEntries");
    }

    /**
     * Restituisce il percorso di output per le conversioni riuscite.
     *
     * @return percorso come stringa
     */
    public static String getSuccessOutputDir() throws JsonStructureException {
        return configDataMap.get("successOutputDir").toString();
    }

    /**
     * Restituisce il percorso di output per le conversioni fallite.
     *
     * @return percorso come stringa
     */
    public static String getErrorOutputDir() throws JsonStructureException {
        return configDataMap.get("errorOutputDir").toString();
    }

    /**
     * Restituisce il percorso della directory monitorata.
     *
     * @return percorso della cartella da monitorare
     */
    public static String getMonitoredDir() throws JsonStructureException {
        return configDataMap.get("monitoredDir").toString();
    }

    /**
     * Indica se il monitoraggio è abilitato all’avvio.
     *
     * @return {@code true} se il monitoraggio parte subito, altrimenti {@code false}
     */
    public static Boolean getIsMonitoringEnabledAtStart() throws JsonStructureException {
        return (Boolean) configDataMap.get("monitorAtStart");
    }

    /**
     * Indica se la conversione multipla è abilitata.
     *
     * @return {@code true} se sono consentite conversioni multiple, {@code false} altrimenti
     */
    public static Boolean getIsMultipleConversionEnabled() throws JsonStructureException {
        return (Boolean) configDataMap.get("multipleConversion");
    }
}
