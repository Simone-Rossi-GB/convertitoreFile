package configuration.configHandlers.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.configExceptions.JsonStructureException;
import configuration.configUtilities.JsonReader;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class InstanceConfigReader extends BaseConfigReader implements JsonReader {
    private File jsonFile;

    public InstanceConfigReader(File jsonFile) {
        super(jsonFile, new AtomicReference<ObjectNode>(null));
    }

    /**
     * Restituisce la lista dei formati che supportano il canale alfa (trasparenza).
     *
     * @return lista di stringhe con i formati che supportano la trasparenza, null nel caso di lettura fallita
     * @throws JsonStructureException se il file JSON è mancante o la chiave non è presente
     */
    public List<String> readFormatsWithAlphaChannel() throws JsonStructureException {
        return JsonReader.read(new TypeReference<List<String>>() {}, "formatsWithAlphaChannel", jsonFile, rootReference);
    }

    /**
     * Restituisce la lista dei formati che richiedono una conversione intermedia.
     *
     * @return lista di formati che necessitano di passaggi intermedi, null nel caso di lettura fallita
     * @throws JsonStructureException se il file JSON è corrotto o manca la chiave di riferimento
     */
    public List<String> readFormatsRequiringIntermediateConversion() throws JsonStructureException {
        return JsonReader.read(new TypeReference<List<String>>() {}, "getFormatsRequiringIntermediateConversion", jsonFile, rootReference);
    }

    /**
     * Restituisce la mappa delle conversioni tra formati definite nel file di configurazione.
     * La struttura della mappa è composta da coppie {@code formatoSorgente -> (formatoDestinazione -> nomeClasseConverter)}.
     *
     * @return mappa nidificata con le possibili conversioni e i rispettivi converter, null nel caso di lettura fallita
     * @throws JsonStructureException se la chiave "conversions" è mancante o non leggibile
     */
    public Map<String, Map<String, String>> readConversions() throws JsonStructureException {
        return JsonReader.read(new TypeReference<Map<String, Map<String, String>>>() {}, "conversions", jsonFile, rootReference);
    }

    public List<String> readMandatoryEntries() throws JsonStructureException {
        return JsonReader.read(new TypeReference<List<String>>() {}, "mandatoryEntries", jsonFile, rootReference);
    }

    /**
     * Restituisce il percorso della directory di output per le conversioni andate a buon fine.
     *
     * @return path della directory di successo come stringa, null nel caso di lettura fallita
     * @throws JsonStructureException se la chiave "successOutputDir" è assente o non leggibile
     */
    public String readSuccessOutputDir() throws JsonStructureException {
        return JsonReader.read(new TypeReference<String>() {}, "successOutputDir", jsonFile, rootReference);
    }

    /**
     * Restituisce il percorso della directory di output per i file che hanno generato errori.
     *
     * @return path della directory degli errori come stringa, null nel caso di lettura fallita
     * @throws JsonStructureException se la chiave "errorOutputDir" è assente o non accessibile
     */
    public String readErrorOutputDir() throws JsonStructureException {
        return JsonReader.read(new TypeReference<String>() {}, "errorOutputDir", jsonFile, rootReference);
    }

    /**
     * Restituisce il percorso della directory monitorata dall'applicazione per l'ingresso dei file.
     *
     * @return path della directory di input, null nel caso di lettura fallita
     * @throws JsonStructureException se la chiave "monitoredDir" non è disponibile
     */
    public String readMonitoredDir() throws JsonStructureException {
        return JsonReader.read(new TypeReference<String>() {}, "monitoredDir", jsonFile, rootReference);
    }

    /**
     * Indica se il monitoraggio della directory di input deve iniziare subito all'avvio.
     *
     * @return true se l'applicazione deve iniziare a monitorare appena avviata, false altrimenti, null nel caso di lettura fallita
     * @throws JsonStructureException se la chiave "monitorAtStart" è assente o non compatibile
     */
    public Boolean readIsMonitoringEnabledAtStart() throws JsonStructureException {
        return JsonReader.read(new TypeReference<Boolean>() {}, "monitorAtStart", jsonFile, rootReference);
    }
}
