package webService.client.configuration.configHandlers.conversionContext;

import com.fasterxml.jackson.databind.node.ObjectNode;
import webService.client.configuration.jsonUtilities.JsonWriter;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

public class InstanceConversionContextWriter {
    /** Riferimento atomico al nodo radice del file JSON, utile per aggiornamenti puntuali. */
    private final AtomicReference<ObjectNode> rootReference = new AtomicReference<>(null);

    /** File JSON da modificare. */
    private final File jsonFile;

    /**
     * Costruttore che inizializza la scrittura su un file JSON specifico.
     *
     * @param jsonFile file di configurazione da scrivere
     */
    public InstanceConversionContextWriter(File jsonFile) {
        this.jsonFile = jsonFile;
    }

    /**
     * Scrive un nuovo valore per il formato di destinazione per le conversioni
     *
     * @param newDestinationFormat nuovo valore del campo "destinationFormat"
     */
    public void writeDestinationFormat(String newDestinationFormat) {
        JsonWriter.write(newDestinationFormat, "destinationFormat", jsonFile, rootReference);
    }

    /**
     * Scrive un nuovo valore per la password necessaria ad alcune conversioni
     *
     * @param newPassword nuovo valore del campo "errorOutputDir"
     */
    public void writePassword(String newPassword) {
        JsonWriter.write(newPassword, "password", jsonFile, rootReference);
    }

    /**
     * Scrive un nuovo valore per il flag union per le conversioni
     *
     * @param isUnionEnabled {@code true} unisce le immagini in output
     */
    public void writeIsUnionEnabled(boolean isUnionEnabled) {
        JsonWriter.write(isUnionEnabled, "union", jsonFile, rootReference);
    }

    /**
     * Scrive un nuovo valore per il flag zippedOutput per le conversioni
     *
     * @param isZippedOutput {@code true} comprime in zip i file in output
     */
    public void writeIsZippedOutput(boolean isZippedOutput) {
        JsonWriter.write(isZippedOutput, "zippedOutput", jsonFile, rootReference);
    }

    /**
     * Scrive un nuovo valore per il flag protected per le conversioni
     *
     * @param isProtected {@code true} protegge quando possibile i file in output
     */
    public void writeProtected(boolean isProtected) {
        JsonWriter.write(isProtected, "protected", jsonFile, rootReference);
    }

    public void writeWatermark(String watermark) {
        JsonWriter.write(watermark, "watermark", jsonFile, rootReference);
    }

    /**
     * Scrive la preferenza per abilitare o meno le conversioni multiple.
     *
     * @param isMonitoringEnabledAtStart valore da scrivere nel campo "multipleConversion"
     */
    public void writeIsMultipleConversionEnabled(boolean isMonitoringEnabledAtStart) {
        JsonWriter.write(isMonitoringEnabledAtStart, "multipleConversion", jsonFile, rootReference);
    }

    /**
     * Scrive la preferenza per abilitare o meno le conversioni multiple.
     *
     * @param token valore da inserire nel campo "token"
     */
    public void writeToken(String token) {
        JsonWriter.write(token, "token", jsonFile, rootReference);
    }
}
