package webService.configuration.configExceptions;

import configuration.configExceptions.JsonException;

public class JsonWriteException extends JsonException {
  public JsonWriteException(String message) {
    super(message);
  }
}
