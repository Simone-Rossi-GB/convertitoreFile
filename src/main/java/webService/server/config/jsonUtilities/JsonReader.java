package webService.server.config.jsonUtilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import webService.server.config.configExceptions.JsonFileNotFoundException;
import webService.server.config.configExceptions.JsonStructureException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility per la lettura di valori da file JSON mediante Jackson.
 * <p>
 * Fornisce metodi statici per recuperare contenuti tipizzati tramite {@link TypeReference},
 * mantenendo un riferimento condiviso al nodo radice ({@code ObjectNode}) del documento.
 */
public interface JsonReader {

    /** Logger per la gestione di messaggi diagnostici ed errori. */
    Logger logger = LogManager.getLogger(JsonReader.class);

    /** Mapper Jackson condiviso per la serializzazione/deserializzazione. */
    ObjectMapper mapper = new ObjectMapper();

    /**
     * Legge un valore associato a una chiave da un file JSON e lo converte in un tipo generico.
     * <p>
     * Usa {@link JsonUtility#checkBuild(File, AtomicReference)} per garantire che il nodo radice sia inizializzato.
     *
     * @param typeRef riferimento al tipo di ritorno, incluso supporto a strutture generiche
     * @param key chiave da cercare nel JSON
     * @param jsonFile file di configurazione da leggere
     * @param rootReference riferimento mutabile al nodo radice (viene popolato al primo accesso)
     * @param <T> tipo del valore restituito
     * @return valore deserializzato del tipo T, oppure {@code null} se la chiave non esiste o si verifica un errore
     */
    static <T> T read(TypeReference<T> typeRef, String key, File jsonFile, AtomicReference<ObjectNode> rootReference) {
        try {
            JsonUtility.checkBuild(jsonFile, rootReference);
            JsonNode value;
            if (key.isEmpty()){
                value = rootReference.get().get("data");
                key = "data"; // Metto a "data" per il logger piu avanti
            } else {
                value = rootReference.get().get("data").get(key);
            }

            if (value == null) {
                logger.error("Lettura del campo \"{}\" da {} fallita (chiave non trovata)", key, jsonFile.getName());
                return null;
            }

            logger.info("Lettura \"{}\" da {} completata", key, jsonFile.getName());
            return mapper.convertValue(value, typeRef);

        } catch (JsonFileNotFoundException | JsonStructureException e) {
            logger.error("Errore durante la lettura del file {}: {}", jsonFile.getPath(), e.getMessage());
            return null;
        }
    }

    /**
     * Restituisce l’intero contenuto di un file JSON come stringa grezza.
     *
     * @param jsonFile file da leggere
     * @return contenuto testuale del file oppure {@code null} in caso di errore di lettura
     */
    static String returnJsonAsString(File jsonFile) {
        try {
            return new String(Files.readAllBytes(jsonFile.toPath()));
        } catch (IOException e) {
            logger.error("Lettura del file {} fallita: {}", jsonFile.getPath(), e.getMessage());
            return null;
        }
    }
}
