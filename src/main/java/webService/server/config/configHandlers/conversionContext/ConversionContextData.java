package webService.server.config.configHandlers.conversionContext;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import webService.server.config.jsonUtilities.JsonData;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Classe base astratta che fornisce un contesto di conversione isolato per ciascun thread,
 * inizializzato da un template statico caricato da un file JSON.
 *
 * FIXED: Risolto problema di inizializzazione ThreadLocal
 */
public abstract class ConversionContextData implements JsonData {
    private static final Logger logger = LogManager.getLogger(ConversionContextData.class);

    /**
     * File JSON contenente la configurazione base del contesto di conversione.
     */
    private static volatile File jsonFile = null;

    /**
     * Nodo radice JSON in cui viene caricato il contenuto del file.
     */
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>();

    /**
     * Contesto locale per ciascun thread.
     * FIXED: Inizializzazione lazy e thread-safe
     */
    protected static final ThreadLocal<HashMap<String, Object>> context = new ThreadLocal<HashMap<String, Object>>() {
        @Override
        protected HashMap<String, Object> initialValue() {
            logger.debug("Inizializzazione ThreadLocal context per thread: {}", Thread.currentThread().getName());
            if (jsonFile != null) {
                HashMap<String, Object> data = JsonData.readData(jsonFile, rootReference);
                logger.debug("Context caricato da file: {} - Dati: {}", jsonFile.getAbsolutePath(), data);
                return data != null ? data : new HashMap<>();
            } else {
                logger.warn("jsonFile è null, ritorno HashMap vuoto");
                return new HashMap<>();
            }
        }
    };

    /**
     * Aggiorna il contesto con una nuova istanza di configurazione.
     * FIXED: Gestione thread-safe e pulizia ThreadLocal
     */
    public static synchronized void update(ConversionContextInstance conversionContextInstance) {
        logger.info("=== INIZIO UPDATE ConversionContextData ===");

        try {
            if (conversionContextInstance == null) {
                logger.error("ConversionContextInstance è null!");
                return;
            }

            File newJsonFile = conversionContextInstance.getJsonFile();
            if (newJsonFile == null) {
                logger.error("JsonFile è null nell'istanza!");
                return;
            }

            if (!newJsonFile.exists()) {
                logger.error("File JSON non esiste: {}", newJsonFile.getAbsolutePath());
                return;
            }

            logger.info("Aggiornamento file da: {} a: {}",
                    jsonFile != null ? jsonFile.getAbsolutePath() : "null",
                    newJsonFile.getAbsolutePath());

            // Aggiorna il file di riferimento
            jsonFile = newJsonFile;

            // IMPORTANTE: Pulisci il ThreadLocal corrente per forzare reload
            context.remove();
            logger.info("ThreadLocal context rimosso e resetted");

            // Test immediato del caricamento
            HashMap<String, Object> testData = context.get();
            logger.info("Test caricamento context: {} elementi caricati", testData.size());
            logger.info("Chiavi disponibili: {}", testData.keySet());

            logger.info("=== UPDATE COMPLETATO CON SUCCESSO ===");

        } catch (Exception e) {
            logger.error("ERRORE durante update ConversionContextData: ", e);
            throw e; // Re-lancia l'eccezione per far fallire la richiesta
        }
    }

    /**
     * Ottiene il file JSON attualmente configurato.
     */
    public static File getJsonFile() {
        return jsonFile;
    }

    /**
     * Ottiene il context del thread corrente.
     * FIXED: Gestione sicura con null check
     */
    protected static HashMap<String, Object> getCurrentContext() {
        HashMap<String, Object> currentContext = context.get();
        if (currentContext == null || currentContext.isEmpty()) {
            logger.warn("Context vuoto per thread {}, file configurato: {}",
                    Thread.currentThread().getName(),
                    jsonFile != null ? jsonFile.getAbsolutePath() : "null");
        }
        return currentContext;
    }

    /**
     * Pulisce il ThreadLocal per il thread corrente.
     * Utile per evitare memory leak.
     */
    public static void cleanup() {
        logger.debug("Cleanup ThreadLocal per thread: {}", Thread.currentThread().getName());
        context.remove();
    }
}