package webService.server;

class ErrorResponse {
    private String operation;
    private String errorCode;
    private String message;
    private String technicalMessage;

    public ErrorResponse(String operation, String errorCode, String message, String technicalMessage) {
        this.operation = operation;
        this.errorCode = errorCode;
        this.message = message;
        this.technicalMessage = technicalMessage;
    }

    // Getters
    public String getOperation() { return operation; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public String getTechnicalMessage() { return technicalMessage; }
}