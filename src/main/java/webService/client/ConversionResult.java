package webService.client;

import java.io.File;

public class ConversionResult {

    private final boolean success;
    private final String message;
    private final String error; // Questo è il campo per gli errori
    private final File result; //riferimento al file convertito;

    // Costruttore principale che accetta success, message, error e file result
    public ConversionResult(boolean success, String message, String error, File result) {
        this.success = success;
        this.message = message;
        this.error = error;
        this.result = result;
    }

    // NUOVO COSTRUTTORE con solo success e message.
    // Inizializza 'error' a null, poiché non c'è un errore specifico per i messaggi di successo.
    public ConversionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.error = null;
        this.result = null;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public boolean hasError() {
        return error != null && !error.trim().isEmpty();
    }

    public String getStatusMessage() {
        return success ? message : error;
    }

    public File getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "ConversionResult{" +
                "success=" + success +
                ", message='" + (success ? message : error) + "'" +
                "}";
    }
}