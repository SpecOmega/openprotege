package com.specomega.openprotege.server.ontology.reasoning;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
class ReasoningErrorHandler {
    @ExceptionHandler(ReasoningException.class)
    ResponseEntity<ErrorResponse> handle(ReasoningException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse("error", exception.errorCode(), exception.getMessage(), Instant.now()));
    }

    record ErrorResponse(String status, String errorCode, String message, Instant timestamp) {}
}
