package com.specomega.openprotege.server.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.specomega.openprotege.server.project.ProjectService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiChatService {
    private static final String SYSTEM_PROMPT = """
            You are the optional OpenProtege ontology assistant.
            Give concise, clearly qualified suggestions. Do not claim to have changed or saved an ontology.
            Users must review and explicitly approve any proposed change in a separate workflow.
            """;

    private final RestClient restClient;
    private final ProjectService projectService;
    private final boolean enabled;
    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;

    public AiChatService(RestClient restClient,
                         ProjectService projectService,
                         @Value("${openprotege.ai.enabled:false}") boolean enabled,
                         @Value("${openprotege.ai.provider:openai-compatible}") String provider,
                         @Value("${openprotege.ai.base-url:https://api.deepseek.com/v1}") String baseUrl,
                         @Value("${openprotege.ai.api-key:}") String apiKey,
                         @Value("${openprotege.ai.model:deepseek-chat}") String model) {
        this.restClient = restClient;
        this.projectService = projectService;
        this.enabled = enabled;
        this.provider = provider;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
    }

    public AiStatus status() {
        return new AiStatus(enabled && !apiKey.isBlank(), provider, model);
    }

    public ChatResponse chat(UUID projectId, String message, List<AiChatController.ChatMessage> history,
                             Authentication actor) {
        if (projectId != null) {
            projectService.get(projectId, actor);
        }
        if (!status().configured()) {
            throw AiChatException.notConfigured();
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        if (history != null) {
            history.forEach(entry -> messages.add(Map.of("role", entry.role(), "content", entry.content())));
        }
        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("stream", false);

        JsonNode response;
        try {
            response = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            throw AiChatException.providerRejected(exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            throw AiChatException.providerUnavailable();
        }

        JsonNode content = response == null ? null : response.path("choices").path(0).path("message").path("content");
        if (content == null || !content.isTextual() || content.asText().isBlank()) {
            throw AiChatException.invalidProviderResponse();
        }
        return new ChatResponse(content.asText());
    }

    private static String stripTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record AiStatus(boolean configured, String provider, String model) {}
    public record ChatResponse(String answer) {}
}
