package webService.configuration.configExceptions;

import configuration.configExceptions.JsonException;

public class NullJsonValueException extends JsonException {
    public NullJsonValueException(String message) {
        super(message);
    }
}
