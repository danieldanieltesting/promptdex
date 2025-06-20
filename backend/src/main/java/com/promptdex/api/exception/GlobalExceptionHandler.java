package com.promptdex.api.exception;

import com.promptdex.api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException; // <-- IMPORT THIS
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- START OF NEW HANDLER ---
    // Handler for bad login credentials (wrong username/password)
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, WebRequest request) {
        // We return a 401 Unauthorized status, which is the correct code for this error.
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Invalid username or password", Instant.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
    // --- END OF NEW HANDLER ---

    // Handler for our custom "Not Found" exception
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage(), Instant.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // Handler for registration errors (username/email already taken)
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        // This is often a 400 Bad Request, but could also be a 409 Conflict depending on the context.
        // For registration (e.g., "username taken"), 409 might be better. Let's stick to BAD_REQUEST for now.
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage(), Instant.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Handler for duplicate review submission conflict
    @ExceptionHandler(ReviewAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleReviewAlreadyExistsException(ReviewAlreadyExistsException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage(), Instant.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // Handler for security-related access denied errors (e.g. trying to edit someone else's prompt)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.FORBIDDEN.value(), "Access Denied: You do not have permission to perform this action.", Instant.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    // Handler for validation failures (@Valid) on DTOs
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed: " + errors, Instant.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // A general fallback handler for any other unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        // Log the full stack trace for debugging purposes
        logger.error("An unexpected error occurred: ", ex);
        ErrorResponse errorResponse = new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "An internal server error occurred. Please try again later.", Instant.now());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}