package webService.client.configuration.configHandlers.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import webService.client.configuration.configExceptions.JsonStructureException;
import webService.client.configuration.jsonUtilities.JsonReader;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Classe che consente la lettura dei dati di configurazione da un file JSON specifico.
 * <p>
 * Utilizza {@link JsonReader} per accedere ai valori contenuti nel file, mantenendo
 * un riferimento condiviso al nodo radice per evitare letture ripetute.
 * <p>
 * È progettata per lavorare con una singola istanza di configurazione (per file).
 */
public class InstanceConfigReader {

    /** Nodo radice del file JSON mantenuto in memoria per evitare parsing multipli. */
    private final AtomicReference<ObjectNode> rootReference = new AtomicReference<>(null);

    /** File di configurazione da cui leggere i parametri. */
    private final File jsonFile;

    /**
     * Costruttore che inizializza il lettore per un file JSON specifico.
     *
     * @param jsonFile file JSON di configurazione
     */
    public InstanceConfigReader(File jsonFile) {
        this.jsonFile = jsonFile;
    }

    /**
     * Restituisce la lista dei campi obbligatori richiesti nella configurazione.
     *
     * @return lista di stringhe rappresentanti le chiavi obbligatorie
     */
    public List<String> readMandatoryEntries() throws JsonStructureException {
        return JsonReader.read(new TypeReference<List<String>>() {}, "mandatoryEntries", jsonFile, rootReference);
    }

    /**
     * Restituisce il percorso di output per le conversioni riuscite.
     *
     * @return percorso come stringa
     */
    public String readSuccessOutputDir() throws JsonStructureException {
        return JsonReader.read(new TypeReference<String>() {}, "successOutputDir", jsonFile, rootReference);
    }

    /**
     * Restituisce il percorso di output per i file non convertiti o con errore.
     *
     * @return percorso come stringa
     */
    public String readErrorOutputDir() throws JsonStructureException {
        return JsonReader.read(new TypeReference<String>() {}, "errorOutputDir", jsonFile, rootReference);
    }

    /**
     * Restituisce la directory che deve essere monitorata.
     *
     * @return percorso monitorato
     */
    public String readMonitoredDir() throws JsonStructureException {
        return JsonReader.read(new TypeReference<String>() {}, "monitoredDir", jsonFile, rootReference);
    }

    /**
     * Indica se il monitoraggio deve essere attivato automaticamente all'avvio.
     *
     * @return {@code true} se il monitoraggio è attivo all'inizio, altrimenti {@code false}
     */
    public Boolean readIsMonitoringEnabledAtStart() throws JsonStructureException {
        return JsonReader.read(new TypeReference<Boolean>() {}, "monitorAtStart", jsonFile, rootReference);
    }


}
