package webService.server.configuration.configExceptions;

import webService.server.configuration.configExceptions.JsonException;

public class JsonFileNotFoundException extends JsonException {
    public JsonFileNotFoundException(String message) {
        super(message);
    }
}
