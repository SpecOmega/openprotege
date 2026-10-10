package com.specomega.openprotege.server.ontology;

import org.springframework.http.HttpStatus;

final class OntologyException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    private OntologyException(String errorCode, HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.status = status;
    }

    String errorCode() {
        return errorCode;
    }

    HttpStatus status() {
        return status;
    }

    static OntologyException unsupportedFormat(String format) {
        return new OntologyException("FORMAT_NOT_SUPPORTED", HttpStatus.BAD_REQUEST,
                "Unsupported ontology format: " + format + ". Supported formats: RDF/XML, Turtle.", null);
    }

    static OntologyException fileSizeExceeded(long actual, long maximum) {
        return new OntologyException("FILE_SIZE_EXCEEDED", HttpStatus.PAYLOAD_TOO_LARGE,
                "Ontology file size " + actual + " exceeds the configured maximum " + maximum + " bytes.", null);
    }

    static OntologyException parsingFailure(Throwable cause) {
        return new OntologyException("PARSING_ERROR", HttpStatus.BAD_REQUEST,
                "The ontology file could not be parsed as RDF/XML or Turtle.", cause);
    }

    static OntologyException versionNotFound() {
        return new OntologyException("VERSION_NOT_FOUND", HttpStatus.NOT_FOUND,
                "Ontology version not found.", null);
    }

    static OntologyException storageFailure(Throwable cause) {
        return new OntologyException("STORAGE_ERROR", HttpStatus.INTERNAL_SERVER_ERROR,
                "The ontology version could not be stored.", cause);
    }
}
