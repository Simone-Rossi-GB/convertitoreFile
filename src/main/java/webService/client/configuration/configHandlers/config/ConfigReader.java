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

}
