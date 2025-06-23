package configuration.jsonUtilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configuration.configExceptions.JsonFileNotFoundException;
import configuration.configExceptions.JsonStructureException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface JsonUtility {
    Logger logger = LogManager.getLogger(JsonUtility.class);

    ObjectMapper mapper = new ObjectMapper();

    static void validateJsonFromString(String jsonText) {
        validateJsonFromString(jsonText, null);
    }

    static void validateJsonFromString(String jsonText, List<String> mandatoryEntries) throws JsonStructureException {
        ObjectMapper mapper = new ObjectMapper();
        if (mandatoryEntries == null){
            mandatoryEntries = new ArrayList<>();
        }
        try {
            JsonNode root = mapper.readTree(jsonText); // verifica che sia JSON valido
            List<String> missingEntries = new ArrayList<>();
            for (String entry : mandatoryEntries){
                if (!root.has(entry)) {
                    missingEntries.add(entry);
                }
            }
            if (!missingEntries.isEmpty()){
                throw new JsonStructureException("JSON mancante di campi obbligatori: "+String.join(", ", missingEntries));
            }
            logger.info("JSON valido");
        } catch (JsonProcessingException | JsonStructureException e) {
            logger.error("JSON non valido: {}", e.getMessage());
            throw new JsonStructureException("JSON non valido: "+e.getMessage());
        }
    }
    /**
     * Verifica se il file JSON esiste e se il nodo radice è già stato caricato.
     * In caso contrario, ne avvia il caricamento.
     *
     * @param jsonFile      file JSON da controllare
     * @param rootReference riferimento al nodo radice, che verrà popolato se ancora nullo
     */
    static void checkBuild(File jsonFile, AtomicReference<ObjectNode> rootReference) throws JsonFileNotFoundException {
        if (jsonFile.exists()) {
            if (rootReference.get() == null) {
                loadJson(jsonFile, rootReference);
            }
        } else {
            logger.error("File {} non trovato", jsonFile.getPath());
            throw new JsonFileNotFoundException("File "+jsonFile.getPath()+" non trovato");
        }
    }

    /**
     * Carica il file JSON e aggiorna il riferimento al nodo radice.
     * Il contenuto viene impostato direttamente tramite il metodo .set(...) su AtomicReference,
     * permettendo la simulazione del passaggio per riferimento anche in metodi statici.
     *
     * @param jsonFile file JSON da leggere
     * @param rootReference contenitore del nodo radice da aggiornare
     */
    static void loadJson(File jsonFile, AtomicReference<ObjectNode> rootReference) throws JsonStructureException {
        try {
            rootReference.set((ObjectNode) mapper.readTree(jsonFile));
        } catch (IOException e) {
            logger.error("Errore durante la lettura di {}", jsonFile.getPath());
            throw new JsonStructureException("Caricamento jsonFile.getName() fallito");
        }
    }
}
