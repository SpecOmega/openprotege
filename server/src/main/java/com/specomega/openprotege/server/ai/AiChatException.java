package com.specomega.openprotege.server.ai;

import org.springframework.http.HttpStatus;

final class AiChatException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    private AiChatException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    static AiChatException notConfigured() {
        return new AiChatException(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED",
                "The AI assistant is not configured on this server.");
    }

    static AiChatException providerRejected(int statusCode) {
        return new AiChatException(HttpStatus.BAD_GATEWAY, "AI_PROVIDER_ERROR",
                "The configured AI provider rejected the request (HTTP " + statusCode + ").");
    }

    static AiChatException providerUnavailable() {
        return new AiChatException(HttpStatus.SERVICE_UNAVAILABLE, "AI_PROVIDER_UNAVAILABLE",
                "The configured AI provider could not be reached.");
    }

    static AiChatException invalidProviderResponse() {
        return new AiChatException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE",
                "The configured AI provider returned an invalid response.");
    }

    HttpStatus status() {
        return status;
    }

    String errorCode() {
        return errorCode;
    }
}
