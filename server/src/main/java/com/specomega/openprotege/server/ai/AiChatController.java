package com.specomega.openprotege.server.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/ai")
public class AiChatController {
    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @GetMapping("/status")
    AiChatService.AiStatus status() {
        return aiChatService.status();
    }

    @PostMapping("/projects/{projectId}/chat")
    ResponseEntity<AiChatService.ChatResponse> chatForProject(
            @PathVariable UUID projectId,
            @Valid @RequestBody ChatRequest request,
            Authentication actor) {
        return ResponseEntity.ok(aiChatService.chat(projectId, request.message(), request.history(), actor));
    }

    @PostMapping("/chat")
    ResponseEntity<AiChatService.ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            Authentication actor) {
        return ResponseEntity.ok(aiChatService.chat(null, request.message(), request.history(), actor));
    }

    record ChatRequest(@NotBlank @Size(max = 4000) String message,
                       @Size(max = 20) List<@Valid ChatMessage> history) {}

    record ChatMessage(@NotBlank @Pattern(regexp = "user|assistant") String role,
                       @NotBlank @Size(max = 4000) String content) {}
}
