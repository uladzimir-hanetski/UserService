package org.example.userserv.util;

import jakarta.validation.ConstraintViolationException;
import org.example.userserv.exception.CardNotFoundException;
import org.example.userserv.exception.ErrorResponse;
import org.example.userserv.exception.UserNotFoundException;
import org.example.userserv.exception.ValueAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        String error = errors.entrySet().stream().map(e ->
                e.getKey() + ": " + e.getValue()).collect(Collectors.joining("; "));
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST,
                "Validation failed", error, request.getDescription(false));

        return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND, "User not found",
                ex.getMessage(), request.getDescription(false));

        return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCardNotFoundException(
            CardNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND, "Card not found",
                ex.getMessage(), request.getDescription(false));

        return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
    }

    @ExceptionHandler(ValueAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleValueAlreadyExistsException(
            ValueAlreadyExistsException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT, "Value already exists",
                ex.getMessage(), request.getDescription(false));

        return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        String errors = ex.getConstraintViolations().stream().map(e ->
                e.getPropertyPath() + ": " + e.getMessage()).collect(Collectors.joining("; "));
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST,
                "Invalid request parameters", errors, request.getDescription(false));

        return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                ex.getMessage(), request.getDescription(false)
        );

        return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
    }
}
