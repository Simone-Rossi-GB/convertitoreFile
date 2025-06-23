package configuration.jsonUtilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configExceptions.JsonFileNotFoundException;
import configuration.configExceptions.JsonStructureException;
import configuration.jsonUtilities.RecognisedWrappers.RecognisedInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utility JSON che fornisce metodi per validare e caricare contenuti JSON
 * da input riconosciuti come {@code File} o {@code String}, supportando la struttura
 * del progetto attraverso un parser Jackson condiviso.
 */
public interface JsonUtility {

    /** Logger per la tracciabilità di eventi e anomalie durante la manipolazione JSON. */
    Logger logger = LogManager.getLogger(JsonUtility.class);

    /** ObjectMapper Jackson condiviso per lettura e parsing JSON. */
    ObjectMapper mapper = new ObjectMapper();

    /**
     * Esegue la validazione della struttura JSON da una sorgente {@link RecognisedInput},
     * senza specificare campi obbligatori.
     *
     * @param jsonText input riconosciuto da validare
     */
    static <T extends RecognisedInput> void validateJsonFromStringOrFile(T jsonText) {
        validateJsonFromStringOrFile(jsonText, null);
    }

    /**
     * Valida un input JSON verificando la sua correttezza strutturale e la presenza dei campi obbligatori.
     * <p>
     * Tenta di leggere il contenuto come {@code File}, fallisce in fallback secondario — ma
     * attualmente entrambe le chiamate usano {@code getValue()} come se fosse un {@code File},
     * il che potrebbe generare ambiguità se supporti anche {@code String}.
     *
     * @param json input riconosciuto (es. {@code RecognisedFile}, {@code RecognisedString})
     * @param mandatoryEntries lista dei campi obbligatori presenti nel nodo "data"
     * @throws JsonStructureException se il contenuto non è valido o mancano campi obbligatori
     */
    static <T extends RecognisedInput> void validateJsonFromStringOrFile(T json, List<String> mandatoryEntries) throws JsonStructureException {
        if (mandatoryEntries == null) {
            mandatoryEntries = new ArrayList<>();
        }

        try {
            JsonNode root;
            try {
                File tempFile = json.getValue();
                root = mapper.readTree(tempFile);
            } catch (Exception ignored) {
                String tempString = json.getValue();
                root = mapper.readTree(tempString);
            }

            List<String> missingEntries = new ArrayList<>();
            for (String entry : mandatoryEntries) {
                if (!root.get("data").has(entry)) {
                    missingEntries.add(entry);
                }
            }

            if (!missingEntries.isEmpty()) {
                throw new JsonStructureException("JSON mancante di campi obbligatori: " + String.join(", ", missingEntries));
            }

            logger.info("JSON valido");

        } catch (JsonStructureException | IOException e) {
            logger.error("JSON non valido: {}", e.getMessage());
            throw new JsonStructureException("JSON non valido: " + e.getMessage());
        }
    }

    /**
     * Verifica se il file esiste e se il nodo radice è già stato caricato.
     * In caso contrario, ne innesca il caricamento.
     *
     * @param jsonFile file da caricare
     * @param rootReference riferimento atomico al nodo radice che verrà popolato
     * @throws JsonFileNotFoundException se il file non esiste sul file system
     */
    static void checkBuild(File jsonFile, AtomicReference<ObjectNode> rootReference) throws JsonFileNotFoundException {
        if (jsonFile.exists()) {
            if (rootReference.get() == null) {
                loadJson(jsonFile, rootReference);
            }
        } else {
            logger.error("File {} non trovato", jsonFile.getPath());
            throw new JsonFileNotFoundException("File " + jsonFile.getPath() + " non trovato");
        }
    }

    /**
     * Carica il file JSON e aggiorna il nodo radice condiviso usando {@link AtomicReference}.
     * <p>
     * Gestisce il parsing e solleva eccezioni strutturali in caso di errori.
     *
     * @param jsonFile file da leggere
     * @param rootReference riferimento da aggiornare con il nodo radice
     * @throws JsonStructureException se la lettura fallisce o il contenuto non è un {@link ObjectNode}
     */
    static void loadJson(File jsonFile, AtomicReference<ObjectNode> rootReference) throws JsonStructureException {
        try {
            rootReference.set((ObjectNode) mapper.readTree(jsonFile));
        } catch (IOException e) {
            logger.error("Errore durante la lettura di {}", jsonFile.getPath());
            throw new JsonStructureException("Caricamento " + jsonFile.getName() + " fallito");
        }
    }
}
