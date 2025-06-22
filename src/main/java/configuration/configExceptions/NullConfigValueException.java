package configuration.configExceptions;

public class NullConfigValueException extends JsonException {
    public NullConfigValueException(String message) {
        super(message);
    }
}
