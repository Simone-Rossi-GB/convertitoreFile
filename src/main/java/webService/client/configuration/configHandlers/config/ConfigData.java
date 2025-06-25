package webService.client.configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import webService.client.configuration.jsonUtilities.JsonData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Classe astratta base per la gestione dei dati di configurazione provenienti da un file JSON.
 * <p>
 * Implementa l'interfaccia {@link JsonData} e fornisce meccanismi per:
 * <ul>
 *     <li>Salvare il file JSON di riferimento</li>
 *     <li>Caricare la struttura radice come {@link ObjectNode}</li>
 *     <li>Ottenere una mappa dei dati di configurazione</li>
 * </ul>
 */
public abstract class ConfigData implements JsonData {

    /** Riferimento al file JSON di configurazione corrente. */
    private static File jsonFile;

    /**
     * Contenitore thread-safe che mantiene il nodo radice del file JSON.
     * Utilizzato per evitare riletture del file.
     */
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>();

    /** Logger per la gestione di messaggi diagnostici ed errori. */
    private static Logger logger = LogManager.getLogger(ConfigData.class);

    /**
     * Mappa che contiene i dati estratti dal file JSON.
     * Accessibile alle sottoclassi per leggere i valori di configurazione.
     */
    protected static HashMap<String, Object> configDataMap;

    /**
     * Aggiorna il file di configurazione e ricarica i dati nella mappa.
     * Viene tipicamente invocato quando cambia la sorgente di configurazione.
     *
     * @param configInstance istanza contenente il nuovo file di configurazione
     */
    public static void update(ConfigInstance configInstance) {
        jsonFile = configInstance.getJsonFile();
        configDataMap = JsonData.readData(jsonFile, rootReference);
        logger.info("Configurazione aggiornata correttamente da {}", jsonFile.getName());
    }

    /**
     * Restituisce il file JSON attualmente usato come sorgente di configurazione.
     *
     * @return file di configurazione attivo
     */
    public static File getJsonFile() {
        return jsonFile;
    }
}
