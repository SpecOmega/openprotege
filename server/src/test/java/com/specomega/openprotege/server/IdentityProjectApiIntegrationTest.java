package com.specomega.openprotege.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IdentityProjectApiIntegrationTest {
    private static final String ADMIN_PASSWORD = "test-bootstrap-password-123";
    private static final String ALICE_PASSWORD = "alice-test-password-123";
    private static final String BOB_PASSWORD = "bob-test-password-123";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("identity_test")
                    .withUsername("identity_test")
                    .withPassword("test-only-password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("openprotege.bootstrap.admin-email", () -> "admin@example.test");
        registry.add("openprotege.bootstrap.admin-password", () -> ADMIN_PASSWORD);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void invitationsSessionsAndProjectAuthorizationRespectConfirmedRoles() throws Exception {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT platform_admin FROM users WHERE email = 'admin@example.test'", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = 'admin@example.test'", String.class))
                .isNotEqualTo(ADMIN_PASSWORD);
        ApiClient invalidLogin = new ApiClient();
        assertThat(invalidLogin.post("/api/auth/login",
                "{\"email\":\"admin@example.test\",\"password\":\"incorrect-password\"}").status())
                .isEqualTo(401);

        ApiClient admin = new ApiClient();
        admin.login("admin@example.test", ADMIN_PASSWORD);
        Response invitationForAlice = admin.post("/api/admin/invitations",
                "{\"email\":\"Alice@Example.test\"}");
        assertThat(invitationForAlice.status()).isEqualTo(200);
        assertThat(invitationForAlice.cacheControl()).contains("no-store");
        JsonNode aliceInvitation = objectMapper.readTree(invitationForAlice.body());
        String aliceToken = aliceInvitation.get("token").asText();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT token_hash FROM user_invitations WHERE id = ?",
                String.class, UUID.fromString(aliceInvitation.get("id").asText())))
                .isNotEqualTo(aliceToken);

        ApiClient aliceInviteClient = new ApiClient();
        assertThat(aliceInviteClient.post("/api/auth/invitations/accept",
                "{\"token\":\"" + aliceToken + "\",\"password\":\"" + ALICE_PASSWORD + "\"}").status())
                .isEqualTo(201);
        Response secondAcceptance = aliceInviteClient.post("/api/auth/invitations/accept",
                "{\"token\":\"" + aliceToken + "\",\"password\":\"" + ALICE_PASSWORD + "\"}");
        assertThat(secondAcceptance.status()).as(secondAcceptance.body()).isEqualTo(400);

        ApiClient alice = new ApiClient();
        alice.login("alice@example.test", ALICE_PASSWORD);
        assertThat(objectMapper.readTree(alice.get("/api/auth/me").body()).get("email").asText())
                .isEqualTo("alice@example.test");
        assertThat(alice.post("/api/admin/invitations",
                "{\"email\":\"forbidden@example.test\"}").status())
                .isEqualTo(403);

        Response invitationForBob = admin.post("/api/admin/invitations",
                "{\"email\":\"bob@example.test\"}");
        String bobToken = objectMapper.readTree(invitationForBob.body()).get("token").asText();
        assertThat(new ApiClient().post("/api/auth/invitations/accept",
                "{\"token\":\"" + bobToken + "\",\"password\":\"" + BOB_PASSWORD + "\"}").status())
                .isEqualTo(201);
        Response expiredInvitation = admin.post("/api/admin/invitations",
                "{\"email\":\"expired@example.test\"}");
        UUID expiredInvitationId = UUID.fromString(
                objectMapper.readTree(expiredInvitation.body()).get("id").asText());
        jdbcTemplate.update(
                "UPDATE user_invitations SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 second' WHERE id = ?",
                expiredInvitationId);
        String expiredToken = objectMapper.readTree(expiredInvitation.body()).get("token").asText();
        assertThat(new ApiClient().post("/api/auth/invitations/accept",
                "{\"token\":\"" + expiredToken + "\",\"password\":\"expired-user-password-123\"}").status())
                .isEqualTo(400);

        ApiClient bob = new ApiClient();
        bob.login("bob@example.test", BOB_PASSWORD);
        Response team = alice.post("/api/teams", "{\"name\":\"Ontology team\"}");
        assertThat(team.status()).isEqualTo(201);
        UUID teamId = UUID.fromString(objectMapper.readTree(team.body()).get("id").asText());
        assertThat(objectMapper.readTree(alice.get("/api/teams").body()).size()).isEqualTo(1);
        assertThat(alice.get("/api/teams/" + teamId + "/members").status()).isEqualTo(200);
        UUID bobId = userId("bob@example.test");

        assertThat(alice.post("/api/teams/" + teamId + "/members",
                "{\"userId\":\"" + bobId + "\",\"role\":\"MEMBER\"}").status())
                .isEqualTo(201);
        assertThat(bob.get("/api/teams/" + teamId + "/members").status()).isEqualTo(403);
        assertThat(bob.post("/api/teams/" + teamId + "/members",
                "{\"userId\":\"" + bobId + "\",\"role\":\"MEMBER\"}").status())
                .isEqualTo(403);

        Response project = alice.post("/api/projects", "{\"name\":\"Private model\","
                + "\"visibility\":\"PRIVATE\",\"teamId\":\"" + teamId + "\"}");
        assertThat(project.status()).isEqualTo(201);
        UUID projectId = UUID.fromString(objectMapper.readTree(project.body()).get("id").asText());
        ApiClient anonymous = new ApiClient();
        assertThat(bob.post("/api/projects", "{\"name\":\"Not allowed\","
                + "\"visibility\":\"PRIVATE\",\"teamId\":\"" + teamId + "\"}").status())
                .isEqualTo(403);

        assertThat(anonymous.get("/api/projects/" + projectId).status()).isEqualTo(404);
        assertThat(bob.get("/api/projects/" + projectId).status()).isEqualTo(404);
        assertThat(objectMapper.readTree(bob.get("/api/projects").body()).isEmpty()).isTrue();
        assertThat(bob.post("/api/projects/" + projectId + "/members",
                "{\"userId\":\"" + bobId + "\",\"role\":\"VIEWER\"}").status())
                .isEqualTo(403);

        assertThat(alice.post("/api/projects/" + projectId + "/members",
                "{\"userId\":\"" + bobId + "\",\"role\":\"VIEWER\"}").status())
                .isEqualTo(201);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM audit_events
                WHERE event_type = 'PROJECT_MEMBER_SET' AND target_id = ? AND subject_id = ?
                """,
                Integer.class, projectId, bobId))
                .isEqualTo(1);
        Response visibleToBob = bob.get("/api/projects/" + projectId);
        assertThat(visibleToBob.status()).isEqualTo(200);
        assertThat(objectMapper.readTree(visibleToBob.body()).get("role").asText()).isEqualTo("VIEWER");
        assertThat(objectMapper.readTree(bob.get("/api/projects").body()).size()).isEqualTo(1);
        assertThat(bob.get("/api/projects/" + projectId + "/members").status()).isEqualTo(403);
        assertThat(alice.get("/api/projects/" + projectId + "/members").status()).isEqualTo(200);
        assertThat(bob.put("/api/projects/" + projectId,
                "{\"name\":\"Changed by viewer\",\"visibility\":\"PUBLIC\"}").status())
                .isEqualTo(403);

        Response publicProject = alice.post("/api/projects",
                "{\"name\":\"Public model\",\"visibility\":\"PUBLIC\"}");
        UUID publicProjectId = UUID.fromString(objectMapper.readTree(publicProject.body()).get("id").asText());
        assertThat(anonymous.get("/api/projects/" + publicProjectId).status()).isEqualTo(200);
        assertThat(anonymous.post("/api/projects", "{\"name\":\"No access\","
                + "\"visibility\":\"PUBLIC\"}").status()).isIn(401, 403);
        assertThat(alice.postWithoutCsrf("/api/teams", "{\"name\":\"CSRF rejected\"}").status())
                .isEqualTo(403);
        alice.logout();
        assertThat(alice.get("/api/projects/" + projectId).status()).isEqualTo(404);
    }

    private UUID userId(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);
    }

    private record Response(int status, String body, String cacheControl) {}

    private final class ApiClient {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
        private String csrf;

        private ApiClient() {
        }

        void login(String email, String password) throws Exception {
            csrf();
            Response response = post("/api/auth/login",
                    "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
            assertThat(response.status()).isEqualTo(204);
            csrf = null;
            csrf();
        }

        Response get(String path) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
            return send(request);
        }

        Response post(String path, String json) throws Exception {
            if (csrf == null) {
                csrf();
            }
            HttpRequest request = HttpRequest.newBuilder(uri(path))
                    .header("Content-Type", "application/json")
                    .header("X-XSRF-TOKEN", csrf)
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            return send(request);
        }

        Response postWithoutCsrf(String path, String json) throws Exception {
            HttpRequest request = HttpRequest.newBuilder(uri(path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            return send(request);
        }

        Response put(String path, String json) throws Exception {
            if (csrf == null) {
                csrf();
            }
            HttpRequest request = HttpRequest.newBuilder(uri(path))
                    .header("Content-Type", "application/json")
                    .header("X-XSRF-TOKEN", csrf)
                    .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            return send(request);
        }

        void logout() throws Exception {
            Response response = post("/api/auth/logout", "{}");
            assertThat(response.status()).isEqualTo(204);
        }

        private void csrf() throws Exception {
            HttpRequest request = HttpRequest.newBuilder(uri("/api/auth/csrf")).GET().build();
            Response response = send(request);
            assertThat(response.status()).isEqualTo(200);
            csrf = objectMapper.readTree(response.body()).get("token").asText();
            assertThat(cookies.getCookieStore().getCookies().stream()
                    .filter(cookie -> cookie.getName().equals("XSRF-TOKEN"))
                    .map(java.net.HttpCookie::getValue))
                    .contains(csrf);
        }

        private Response send(HttpRequest request) throws Exception {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body(),
                    response.headers().firstValue("cache-control").orElse(""));
        }

        private URI uri(String path) {
            return URI.create("http://localhost:" + port + path);
        }
    }
}
