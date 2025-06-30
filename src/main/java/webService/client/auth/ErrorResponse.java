package webService.client.auth;

/**
 * Risposta di errore
 */
public class ErrorResponse {
    private boolean success;
    private String message;
    private String error;
    private int code;

    public ErrorResponse() {}

    public ErrorResponse(String message) {
        this.success = false;
        this.message = message;
    }

    public ErrorResponse(String message, String error, int code) {
        this.success = false;
        this.message = message;
        this.error = error;
        this.code = code;
    }

    // Getters e Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
}