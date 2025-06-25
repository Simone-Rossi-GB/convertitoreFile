package webService.configuration.configExceptions;

import configuration.configExceptions.JsonException;

public class JsonFileNotFoundException extends JsonException {
    public JsonFileNotFoundException(String message) {
        super(message);
    }
}
