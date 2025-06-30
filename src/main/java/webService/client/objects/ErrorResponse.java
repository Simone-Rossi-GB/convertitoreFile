package webService.client.objects;

public class ErrorResponse {
    private int errorCode;
    private String message;
    private String stackTrace;

    public int getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public String getStackTrace() { return stackTrace; }
}
