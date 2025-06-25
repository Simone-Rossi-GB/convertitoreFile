package webService.server.configuration.configExceptions;

import webService.client.configuration.configExceptions.JsonException;

public class NullJsonValueException extends JsonException {
    public NullJsonValueException(String message) {
        super(message);
    }
}
