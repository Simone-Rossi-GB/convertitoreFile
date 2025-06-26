package webService.server;

public class ErrorResponse {
    private int errorCode;
    private String message;
    private String stackTrace;

    public ErrorResponse(int errorCode, String message, String stackTrace) {
        this.errorCode = errorCode;
        this.message = message;
        this.stackTrace = stackTrace;
    }

    // getter e setter (o usa Lombok @Data)
    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
}
