package com.specomega.openprotege.server.ai;

import com.specomega.openprotege.server.project.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class AiChatServiceTest {
    private RestClient.Builder builder;
    private RestClient restClient;
    private MockRestServiceServer server;
    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        projectService = mock(ProjectService.class);
    }

    @Test
    void sendsOpenAiCompatibleChatAndReturnsAnswer() {
        server.expect(requestTo("https://api.example.test/v1/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer unit-test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("Suggest a class"))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"Consider a named class."}}]}
                        """, MediaType.APPLICATION_JSON));

        AiChatService service = new AiChatService(restClient, projectService, true, "test-provider",
                "https://api.example.test/v1/", "unit-test-key", "test-model");

        AiChatService.ChatResponse response = service.chat(null, "Suggest a class", List.of(), null);

        assertThat(response.answer()).isEqualTo("Consider a named class.");
        assertThat(service.status()).isEqualTo(new AiChatService.AiStatus(true, "test-provider", "test-model"));
        server.verify();
    }

    @Test
    void doesNotCallProviderWhenApiKeyIsMissing() {
        AiChatService service = new AiChatService(restClient, projectService, true, "deepseek",
                "https://api.deepseek.com/v1", "", "deepseek-chat");

        assertThat(service.status().configured()).isFalse();
        assertThatThrownBy(() -> service.chat(null, "Hello", List.of(), null))
                .isInstanceOf(AiChatException.class)
                .hasMessageContaining("not configured");
        server.verify();
    }

    @Test
    void disabledProviderIsReportedAsNotConfigured() {
        AiChatService service = new AiChatService(restClient, projectService, false, "deepseek",
                "https://api.deepseek.com/v1", "unit-test-key", "deepseek-chat");

        assertThat(service.status().configured()).isFalse();
        assertThatThrownBy(() -> service.chat(null, "Hello", List.of(), null))
                .isInstanceOf(AiChatException.class);
        server.verify();
    }
}
