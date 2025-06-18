package WebService.client;

public class ConversionResult {

    private final boolean success;
    private final String message;
    private final String error;

    public ConversionResult(boolean success, String message, String error) {
        this.success = success;
        this.message = message;
        this.error = error;
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

    // Factory methods per creare risultati
    public static ConversionResult success(String message) {
        return new ConversionResult(true, message, null);
    }

    public static ConversionResult error(String error) {
        return new ConversionResult(false, null, error);
    }
}