package configuration.configUtilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configuration.configExceptions.JsonFileNotFoundException;
import configuration.configExceptions.JsonStructureException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Classe astratta che fornisce metodi statici per la lettura di valori da file JSON
 * utilizzando Jackson. Supporta la deserializzazione di valori generici (inclusi array)
 * tramite TypeReference, mantenendo il root JSON condiviso attraverso una AtomicReference.
 */
public interface JsonReader extends JsonUtility {
    // Logger per il tracciamento di errori ed eventi
    Logger logger = LogManager.getLogger(JsonReader.class);

    // Mapper di Jackson per la lettura e conversione dei JSON
    ObjectMapper mapper = new ObjectMapper();


    /**
     * Legge un valore associato a una chiave specifica da un file JSON e lo converte nel tipo specificato.
     * Utilizza una TypeReference per supportare anche tipi generici complessi come List<String> o Map<K, V>.
     *
     * @param typeRef       riferimento generico al tipo di ritorno, creato con TypeReference<T> {}
     * @param key           chiave da cercare nel JSON
     * @param jsonFile      file JSON da cui leggere
     * @param rootReference riferimento mutabile al nodo radice; deve essere un final AtomicReference per mantenere lo stesso oggetto
     * @return oggetto deserializzato del tipo T, oppure null in caso di errore o chiave mancante
     * @param <T> tipo del valore che si intende leggere dal JSON
     */
    static <T> T read(TypeReference<T> typeRef, String key, File jsonFile, AtomicReference<ObjectNode> rootReference) {
        try {
            JsonUtility.checkBuild(jsonFile, rootReference);
            JsonNode value = rootReference.get().get(key);
            if (value == null) {
                logger.error("Lettura \"{}\" da {} fallita", key, jsonFile.getName());
                return null;
            }
            // Conversione del nodo JSON in oggetto Java del tipo T usando TypeReference anonimo
            logger.info("Lettura \"{}\" da {} completata", key, jsonFile.getName());
            return mapper.convertValue(value, typeRef);
        } catch (JsonFileNotFoundException | JsonStructureException e) {
            return null;
        }
    }

    static String returnJsonAsString(File jsonFile) {
        try {
            return new String(Files.readAllBytes(jsonFile.toPath()));
        } catch (IOException e) {
            logger.error("Lettura del file {} fallita", jsonFile.getPath());
            return null;
        }
    }
}