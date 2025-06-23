package configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.jsonUtilities.JsonWriter;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Classe responsabile della scrittura di valori nel file di configurazione JSON.
 * <p>
 * Utilizza {@link JsonWriter} per aggiornare i singoli campi e mantiene un riferimento
 * condiviso al nodo radice per evitare parsing multipli.
 */
public class InstanceConfigWriter extends ConfigData {

    /** Riferimento atomico al nodo radice del file JSON, utile per aggiornamenti puntuali. */
    private final AtomicReference<ObjectNode> rootReference = new AtomicReference<>(null);

    /** File JSON da modificare. */
    private final File jsonFile;

    /**
     * Costruttore che inizializza la scrittura su un file JSON specifico.
     *
     * @param jsonFile file di configurazione da scrivere
     */
    public InstanceConfigWriter(File jsonFile) {
        this.jsonFile = jsonFile;
    }

    /**
     * Scrive un nuovo valore per il percorso di output delle conversioni riuscite.
     *
     * @param newSuccessOutputDir nuovo valore del campo "successOutputDir"
     */
    public void writeSuccessOutputDir(String newSuccessOutputDir) {
        JsonWriter.write(newSuccessOutputDir, "successOutputDir", jsonFile, rootReference);
    }

    /**
     * Scrive un nuovo valore per il percorso di output delle conversioni fallite.
     *
     * @param newErrorOutputDir nuovo valore del campo "errorOutputDir"
     */
    public void writeErrorOutputDir(String newErrorOutputDir) {
        JsonWriter.write(newErrorOutputDir, "errorOutputDir", jsonFile, rootReference);
    }

    /**
     * Scrive un nuovo valore per la directory da monitorare.
     *
     * @param newMonitoredDir nuovo valore del campo "monitoredOutputDir"
     */
    public void writeMonitoredDir(String newMonitoredDir) {
        JsonWriter.write(newMonitoredDir, "monitoredOutputDir", jsonFile, rootReference);
    }

    /**
     * Scrive la preferenza per l’avvio automatico del monitoraggio.
     *
     * @param isMonitoringEnabledAtStart {@code true} se il monitoraggio va avviato subito
     */
    public void writeIsMonitoringEnabledAtStart(boolean isMonitoringEnabledAtStart) {
        JsonWriter.write(isMonitoringEnabledAtStart, "monitorAtStart", jsonFile, rootReference);
    }

    /**
     * Scrive la preferenza per abilitare o meno le conversioni multiple.
     *
     * @param isMonitoringEnabledAtStart valore da scrivere nel campo "multipleConversion"
     */
    public void writeIsMultipleConversionEnabled(boolean isMonitoringEnabledAtStart) {
        JsonWriter.write(isMonitoringEnabledAtStart, "multipleConversion", jsonFile, rootReference);
    }
}
