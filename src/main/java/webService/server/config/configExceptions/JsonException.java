package webService.server.config.configExceptions;

public abstract class JsonException extends RuntimeException {
    public JsonException(String message) {
        super(message);
    }
}
