package configuration.configHandlers.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import configuration.jsonUtilities.JsonWriter;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Fornisce metodi per modificare i parametri di configurazione caricati dal file JSON.
 * <p>
 * Estende {@link ConfigData} per accedere alla mappa di configurazione ({@code configDataMap}),
 * e consente di aggiornare dinamicamente i valori principali.
 * <p>
 * Nota: questa classe non scrive i cambiamenti direttamente su disco;
 * per la persistenza è richiesto l’uso di {@link JsonWriter}.
 */
public class ConfigWriter extends ConfigData {

    /**
     * Contenitore condiviso per il nodo radice del file JSON.
     * Sebbene non venga usato direttamente in questa classe, può essere utile
     * per applicazioni che modificano e serializzano la struttura JSON.
     */
    private static final AtomicReference<ObjectNode> rootReference = new AtomicReference<>(null);

    /**
     * Imposta un nuovo percorso per la directory di output delle conversioni riuscite.
     *
     * @param newSuccessOutputDir nuovo valore per "successOutputDir"
     */
    public void setSuccessOutputDir(String newSuccessOutputDir) {
        configDataMap.put("successOutputDir", newSuccessOutputDir);
    }

    /**
     * Imposta un nuovo percorso per la directory di output delle conversioni fallite.
     *
     * @param newErrorOutputDir nuovo valore per "errorOutputDir"
     */
    public void setErrorOutputDir(String newErrorOutputDir) {
        configDataMap.put("errorOutputDir", newErrorOutputDir);
    }

    /**
     * Imposta il nuovo percorso della directory da monitorare.
     *
     * @param newMonitoredDir nuovo valore per "monitoredDir"
     */
    public void setMonitoredDir(String newMonitoredDir) {
        configDataMap.put("monitoredDir", newMonitoredDir);
    }

    /**
     * Specifica se il monitoraggio deve essere attivo all’avvio.
     *
     * @param isMonitoringEnabledAtStart valore booleano per "monitorAtStart"
     */
    public void setIsMonitoringEnabledAtStart(boolean isMonitoringEnabledAtStart) {
        configDataMap.put("monitorAtStart", isMonitoringEnabledAtStart);
    }

    /**
     * Abilita o disabilita la possibilità di effettuare conversioni multiple.
     *
     * @param isMultipleConversionEnabled valore booleano per "multipleConversion"
     */
    public void setIsMultipleConversionEnabled(boolean isMultipleConversionEnabled) {
        configDataMap.put("multipleConversion", isMultipleConversionEnabled);
    }
}
