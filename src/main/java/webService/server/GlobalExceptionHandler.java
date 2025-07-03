package webService.server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import webService.server.converters.exception.*;

import java.io.IOException;
import java.util.Arrays;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalExtensionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalExtension(IllegalExtensionException ex) {
        return buildErrorResponse(1001, ex.getMessage());
    }


    @ExceptionHandler(FileMoveException.class)
    public ResponseEntity<ErrorResponse> handleFileMove(FileMoveException ex) {
        return buildErrorResponse(1002, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedConversionException.class)
    public ResponseEntity<ErrorResponse> handleConversion(UnsupportedConversionException ex) {
        return buildErrorResponse(1003, ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIO(IOException ex) {
        return buildErrorResponse(1004, ex.getMessage());
    }

    @ExceptionHandler(BatchConversionException.class)
    public ResponseEntity<ErrorResponse> handleBatchConversion(BatchConversionException ex) {
        return buildErrorResponse(1005, ex.getMessage());
    }

    @ExceptionHandler(ConversionException.class)
    public ResponseEntity<ErrorResponse> handleConversion(ConversionException ex) {
        return buildErrorResponse(1006, ex.getMessage());
    }

    @ExceptionHandler(FileCreationException.class)
    public ResponseEntity<ErrorResponse> handleFileCreation(FileCreationException ex) {
        return buildErrorResponse(1007, ex.getMessage());
    }

    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<ErrorResponse> handleEmptyFile(EmptyFileException ex) {
        return buildErrorResponse(1008, ex.getMessage());
    }

    @ExceptionHandler(FormatsException.class)
    public ResponseEntity<ErrorResponse> handleFormats(FormatsException ex) {
        return buildErrorResponse(1009, ex.getMessage());
    }

    @ExceptionHandler(PasswordException.class)
    public ResponseEntity<ErrorResponse> handlePassword(PasswordException ex) {
        return buildErrorResponse(1010, ex.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(NullPointerException ex) {
        return buildErrorResponse(1011, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return buildErrorResponse(9999, ex.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(int code, String message) {
        ErrorResponse response = new ErrorResponse(code, message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
