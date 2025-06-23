package configuration.jsonUtilities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configuration.configExceptions.JsonFileNotFoundException;
import configuration.configExceptions.JsonStructureException;
import configuration.configExceptions.JsonWriteException;
import configuration.configHandlers.config.ConfigReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public interface JsonWriter extends JsonUtility {
    // Logger per il tracciamento di errori ed eventi
    Logger logger = LogManager.getLogger(JsonWriter.class);

    // Mapper di Jackson per la lettura e conversione dei JSON
    ObjectMapper mapper = new ObjectMapper();

    static <T> void write(T updatedEntry, String key, File jsonFile, AtomicReference<ObjectNode> rootReference){
        try {
            JsonUtility.checkBuild(jsonFile, rootReference);
            JsonNode valueNode = mapper.valueToTree(updatedEntry);
            rootReference.get().set(key, valueNode);
        } catch (JsonFileNotFoundException | JsonStructureException e){
            logger.error("Scrittura della variabile {} su {} fallita", key, jsonFile.getName());
            throw new JsonWriteException(e.getMessage());
        }

        //Scrittura su file
        try (FileWriter writer = new FileWriter(jsonFile)) {
            mapper.writerWithDefaultPrettyPrinter().writeValue(writer, rootReference.get());
            logger.info("Chiave '{}' aggiornata correttamente", key);
        } catch (IOException e) {
            logger.error("Scrittura sul file fallita", e);
            throw new JsonWriteException("Errore nella scrittura del file di configurazione");
        }
    }

    static void overwriteJsonFromString(String jsonText, File jsonFile) throws JsonStructureException, IOException {
        // Prima valida che il JSON sia corretto
        JsonUtility.validateJsonFromString(jsonText, ConfigReader.getSingleton().readMandatoryEntries());
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write(jsonText);
            writer.flush(); // Assicurati che i dati siano scritti
            logger.info("Scrittura su config.json completata");
        } catch (IOException e) {
            logger.error("Scrittura su config.json fallita");
            throw new JsonWriteException("Errore nella scrittura del file di configurazione: " + e.getMessage());
        }
    }
}
