package webService.configuration.configHandlers.conversionContext;

import com.fasterxml.jackson.databind.node.ObjectNode;
import webService.configuration.configHandlers.conversionContext.ConversionContextInstance;
import webService.configuration.jsonUtilities.JsonData;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Classe base astratta che fornisce un contesto di conversione isolato per ciascun thread,
 * inizializzato da un template statico caricato da un file JSON.
 * <p>
 * Utilizza un {@link ThreadLocal} per garantire che ogni thread disponga di una
 * copia indipendente dei dati, utile per conversioni concorrenti o multithreaded.
 */
public abstract class ConversionContextData implements JsonData {

    /**
     * File JSON contenente la configurazione base del contesto di conversione.
     * Viene letto una sola volta all'avvio.
     */
    private static File jsonFile = new File("src/main/java/configuration/configFiles/conversionContext.json");

    /**
     * Nodo radice JSON in cui viene caricato il contenuto del file,
     * evitando così letture ripetute e non necessarie.
     */
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>();

    /**
     * Template statico dei dati di configurazione, letto una sola volta dal file JSON
     * e utilizzato per inizializzare ogni contesto thread-specifico.
     */
    private static final HashMap<String, Object> baseTemplate = JsonData.readData(jsonFile, rootReference);

    /**
     * Contesto locale per ciascun thread.
     * Ogni thread riceve una copia del template iniziale, garantendo isolamento e integrità dei dati.
     */
    protected static ThreadLocal<HashMap<String, Object>> context =
            ThreadLocal.withInitial(() -> new HashMap<>(baseTemplate));

    public static void update(ConversionContextInstance conversionContextInstance) {
        jsonFile = conversionContextInstance.getJsonFile();
        context = ThreadLocal.withInitial(() -> JsonData.readData(jsonFile, rootReference));
    }
    public static File getJsonFile() {
        return jsonFile;
    }
}
