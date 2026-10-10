package com.specomega.openprotege.server.ontology.reasoning;

import org.springframework.http.HttpStatus;

final class ReasoningException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    private ReasoningException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    static ReasoningException profileRejected(int violationCount) {
        return new ReasoningException(HttpStatus.UNPROCESSABLE_ENTITY, "OWL2_DL_PROFILE_VIOLATION",
                "Ontology is outside the OWL 2 DL profile (" + violationCount + " profile violations).");
    }

    static ReasoningException classNotFound(String classIri) {
        return new ReasoningException(HttpStatus.NOT_FOUND, "CLASS_NOT_FOUND",
                "Named class was not found in the selected ontology: " + classIri);
    }

    static ReasoningException invalidClassIri() {
        return new ReasoningException(HttpStatus.BAD_REQUEST, "INVALID_CLASS_IRI",
                "A valid IRI is required for the named class.");
    }

    static ReasoningException tooLarge(long axiomCount, long maximum) {
        return new ReasoningException(HttpStatus.PAYLOAD_TOO_LARGE, "REASONING_AXIOM_LIMIT_EXCEEDED",
                "Ontology has " + axiomCount + " axioms; the configured reasoning limit is " + maximum + ".");
    }

    static ReasoningException busy() {
        return new ReasoningException(HttpStatus.SERVICE_UNAVAILABLE, "REASONER_BUSY",
                "The reasoning worker is busy; retry after the active task finishes.");
    }

    static ReasoningException timedOut(long timeoutSeconds) {
        return new ReasoningException(HttpStatus.GATEWAY_TIMEOUT, "REASONING_TIMEOUT",
                "Reasoning exceeded the configured timeout of " + timeoutSeconds + " seconds.");
    }

    static ReasoningException failed(Throwable cause) {
        return new ReasoningException(HttpStatus.UNPROCESSABLE_ENTITY, "REASONING_FAILED",
                "The ontology reasoner could not complete this analysis.");
    }

    HttpStatus status() {
        return status;
    }

    String errorCode() {
        return errorCode;
    }
}
