package webService.server.configuration.configExceptions;

import webService.client.configuration.configExceptions.JsonException;

public class JsonStructureException extends JsonException {
    public JsonStructureException(String message) {
        super(message);
    }
}
