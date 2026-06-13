package by.chaika19.orderservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException exception) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), exception.getMessage(), Instant.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessError(BusinessException exception) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), exception.getMessage(), Instant.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new HashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(error -> {
                    String fieldName = error.getField();
                    String errorMessage = error.getDefaultMessage();
                    errors.put(fieldName, errorMessage);
                });

        ValidationErrorResponse validationErrorResponse = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                errors,
                Instant.now()
        );
        return new ResponseEntity<>(validationErrorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.error("An unhandled exception was caught", exception);

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected internal server error occurred",
                Instant.now()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public record ErrorResponse(int status, String message, Instant time) {}

    public record ValidationErrorResponse(int status, String message, Map<String, String> errors, Instant time) {}
}
