package com.loopy.carden.exception;

import com.loopy.carden.dto.StandardResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
            .success(false)
            .message(ex.getMessage())
            .build();
            
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<StandardResponse<Void>> handleBadRequestException(
            BadRequestException ex, WebRequest request) {
        log.error("Bad request: {}", ex.getMessage());
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
            .success(false)
            .message(ex.getMessage())
            .build();
            
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        StandardResponse<Map<String, String>> response = StandardResponse.<Map<String, String>>builder()
            .success(false)
            .message("Validation failed")
            .data(errors)
            .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        log.error("Constraint violation: {}", ex.getMessage());
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
            .success(false)
            .message("Constraint violation: " + ex.getMessage())
            .build();
            
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<StandardResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
            .success(false)
            .message("Invalid credentials")
            .build();
            
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<StandardResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
            .success(false)
            .message("Access denied")
            .build();
            
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException ex, WebRequest request) {
        // Log concisely without full stack trace for common 404s
        log.warn("Resource not found: {}", ex.getResourcePath());
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
            .success(false)
            .message("Resource not found")
            .build();
            
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<StandardResponse<Void>> handleInvalidDataAccessApiUsageException(
            InvalidDataAccessApiUsageException ex, WebRequest request) {
        // Log warning for invalid sort/pagination parameters
        log.warn("Invalid data access usage: {}", ex.getMessage());
        
        String message = "Invalid sort parameter. Use valid field names like: title, createdAt, updatedAt";
        if (ex.getMessage().contains("Sort expression")) {
            message = "Invalid sort field. Valid fields: title, description, createdAt, updatedAt, visibility, cefrLevel";
        }
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
            .success(false)
            .message(message)
            .build();
            
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<StandardResponse<Void>> handleInvalidDataAccessResourceUsageException(
            InvalidDataAccessResourceUsageException ex, WebRequest request) {
        // Log warning for SQL errors (like invalid column names)
        log.warn("Invalid data access resource usage: {}", ex.getMessage());
        
        String message = "Invalid query parameter";
        if (ex.getMessage().contains("column") && ex.getMessage().contains("does not exist")) {
            message = "Invalid sort field. Valid fields for decks: title, description, createdAt, updatedAt, visibility, cefrLevel";
        }
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
            .success(false)
            .message(message)
            .build();
            
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error: ", ex);
        
        StandardResponse<Void> response = StandardResponse.<Void>builder()
            .success(false)
            .message("An unexpected error occurred")
            .build();
            
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
