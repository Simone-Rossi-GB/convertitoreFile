package WebService.client;

public class ConversionResult {

    private final boolean success;
    private final String message;
    private final String error; // Questo è il campo per gli errori

    // Costruttore principale che accetta success, message e error
    public ConversionResult(boolean success, String message, String error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }

    // NUOVO COSTRUTTORE con solo success e message.
    // Inizializza 'error' a null, poiché non c'è un errore specifico per i messaggi di successo.
    public ConversionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.error = null;
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

    @Override
    public String toString() {
        return "ConversionResult{" +
                "success=" + success +
                ", message='" + (success ? message : error) + "'" +
                "}";
    }

    // Factory methods per creare risultati (questi sono già corretti)
    public static ConversionResult success(String message) {
        return new ConversionResult(true, message, null);
    }

    public static ConversionResult error(String error) {
        return new ConversionResult(false, null, error);
    }
}