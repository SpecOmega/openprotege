package com.specomega.openprotege.server.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
class AiChatErrorHandler {
    @ExceptionHandler(AiChatException.class)
    ResponseEntity<ErrorResponse> handleAiChatException(AiChatException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse("error", exception.errorCode(), exception.getMessage(), Instant.now()));
    }

    record ErrorResponse(String status, String errorCode, String message, Instant timestamp) {}
}
