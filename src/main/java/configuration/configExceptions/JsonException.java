package configuration.configExceptions;

public abstract class JsonException extends RuntimeException {
    public JsonException(String message) {
        super(message);
    }
}
