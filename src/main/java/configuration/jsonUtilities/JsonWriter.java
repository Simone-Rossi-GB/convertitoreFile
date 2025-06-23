package configuration.jsonUtilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configuration.configExceptions.JsonFileNotFoundException;
import configuration.configExceptions.JsonStructureException;
import configuration.configExceptions.JsonWriteException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility per la scrittura di contenuti JSON su file di configurazione.
 * <p>
 * Fornisce metodi per aggiornare valori specifici o sovrascrivere completamente il contenuto
 * tramite stringa JSON. Utilizza Jackson per gestire la serializzazione e Log4j per il logging.
 */
public interface JsonWriter {

    /** Logger per la tracciabilità delle operazioni di scrittura. */
    Logger logger = LogManager.getLogger(JsonWriter.class);

    /** ObjectMapper condiviso per la conversione oggetti → JSON. */
    ObjectMapper mapper = new ObjectMapper();

    /**
     * Aggiorna una singola chiave del JSON in memoria e scrive il file su disco.
     *
     * @param updatedEntry valore da serializzare
     * @param key chiave da aggiornare nel JSON
     * @param jsonFile file JSON da aggiornare
     * @param rootReference riferimento atomico al nodo radice già caricato (o da caricare)
     * @param <T> tipo del valore da aggiornare
     * @throws JsonWriteException se la scrittura fallisce o il file è corrotto/non esistente
     */
    static <T> void write(T updatedEntry, String key, File jsonFile, AtomicReference<ObjectNode> rootReference) {
        try {
            JsonUtility.checkBuild(jsonFile, rootReference);
            JsonNode valueNode = mapper.valueToTree(updatedEntry);
            rootReference.get().set(key, valueNode);
        } catch (JsonFileNotFoundException | JsonStructureException e) {
            logger.error("Scrittura della variabile {} su {} fallita", key, jsonFile.getName());
            throw new JsonWriteException(e.getMessage());
        }

        // Salvataggio su file
        try (FileWriter writer = new FileWriter(jsonFile)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, rootReference.get());
            logger.info("Chiave '{}' aggiornata correttamente", key);
        } catch (IOException e) {
            logger.error("Scrittura sul file fallita", e);
            throw new JsonWriteException("Errore nella scrittura del file di configurazione");
        }
    }

    /**
     * Sovrascrive completamente il contenuto di un file JSON con una stringa.
     * <p>
     * La validazione sintattica è opzionale (commentata) ma consigliata prima della scrittura.
     *
     * @param jsonText stringa JSON valida da scrivere
     * @param jsonFile file di destinazione
     * @throws JsonWriteException se la scrittura sul file fallisce
     */
    static void overwriteJsonFromString(String jsonText, File jsonFile) throws JsonStructureException {
        // JsonUtility.validateJsonFromStringOrFile(new RecognisedString(jsonText), ConfigReader.getMandatoryEntries());
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(jsonText);
            writer.flush();
            logger.info("Scrittura su config.json completata");
        } catch (IOException e) {
            logger.error("Scrittura su config.json fallita");
            throw new JsonWriteException("Errore nella scrittura del file di configurazione: " + e.getMessage());
        }
    }
}
