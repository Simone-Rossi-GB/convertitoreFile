package webService.controller;

public class ConversionResponse {
    private boolean success;
    private String message;
    private String errorCode;         // E.g., "PASSWORD_REQUIRED", "INVALID_PASSWORD", "UNION_BOOL_REQUIRED"
    public ConversionResponse(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
    }
}
