package com.specomega.openprotege.server.ontology;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;

@RestControllerAdvice
class OntologyErrorHandler {
    @ExceptionHandler(OntologyException.class)
    ResponseEntity<ErrorResponse> handleOntologyException(OntologyException exception) {
        return ResponseEntity.status(exception.status())
                .body(new ErrorResponse("error", exception.errorCode(), exception.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("error", "FILE_SIZE_EXCEEDED",
                        "Ontology upload exceeds the configured maximum file size.", Instant.now()));
    }

    record ErrorResponse(String status, String errorCode, String message, Instant timestamp) {}
}
