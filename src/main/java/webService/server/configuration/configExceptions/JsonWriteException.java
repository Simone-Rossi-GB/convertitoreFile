package webService.server.configuration.configExceptions;

import webService.client.configuration.configExceptions.JsonException;

public class JsonWriteException extends JsonException {
  public JsonWriteException(String message) {
    super(message);
  }
}
