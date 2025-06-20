package com.promptdex.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a user attempts to submit a review for a prompt
 * they have already reviewed.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException(String message) {
        super(message);
    }
}