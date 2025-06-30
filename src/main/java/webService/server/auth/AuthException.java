package webService.server.auth;

/**
 * Eccezione per errori di autenticazione
 */
public class AuthException extends Exception {
  public AuthException(String message) {
    super(message);
  }

  public AuthException(String message, Throwable cause) {
    super(message, cause);
  }
}