package configuration.jsonUtilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configExceptions.JsonStructureException;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Interfaccia che fornisce un metodo statico per leggere dati da un file JSON strutturato,
 * restituendo una mappa generica chiave/valore.
 * <p>
 * Questa interfaccia è pensata per essere implementata da classi che lavorano
 * con contenuti JSON strutturati a partire da un nodo radice standardizzato.
 */
public interface JsonData {

    /**
     * Legge i dati dalla sezione "data" di un file JSON,
     * trasformandoli in una {@link HashMap} con chiavi {@link String} e valori {@link Object}.
     * <p>
     * Utilizza {@link JsonReader} per l'elaborazione e condivide un riferimento atomico
     * al nodo radice per garantire efficienza e sincronizzazione.
     *
     * @param jsonFile il file JSON da cui leggere
     * @param rootReference riferimento mutabile al nodo radice JSON
     * @return mappa contenente i dati letti dalla sezione "data"
     * @throws JsonStructureException se la struttura del file è errata o mancante
     */
    static HashMap<String, Object> readData(File jsonFile, AtomicReference<ObjectNode> rootReference)
            throws JsonStructureException {
        return JsonReader.read(
                new TypeReference<HashMap<String, Object>>() {},
                "data",
                jsonFile,
                rootReference
        );
    }
}
