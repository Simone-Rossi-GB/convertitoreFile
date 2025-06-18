package WebService.controller;

public class ExceptionHandlerController extends RuntimeException {
    public ExceptionHandlerController(String message) {
        super(message);
    }
}
