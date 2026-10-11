package com.specomega.openprotege.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specomega.openprotege.server.project.ProjectService;
import com.specomega.openprotege.server.project.ProjectService.ProjectRole;
import com.specomega.openprotege.server.project.ProjectService.Visibility;
import com.specomega.openprotege.server.team.TeamService;
import com.specomega.openprotege.server.team.TeamService.TeamRole;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"dev", "mysql"})
class MySqlCompatibilityIntegrationTest {
    private static final String ADMIN_EMAIL = "mysql-admin@example.test";
    private static final String ADMIN_PASSWORD = "mysql-test-bootstrap-password-123";

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("openprotege_test")
                    .withUsername("openprotege_test")
                    .withPassword("mysql-test-password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("openprotege.bootstrap.admin-email", () -> ADMIN_EMAIL);
        registry.add("openprotege.bootstrap.admin-password", () -> ADMIN_PASSWORD);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Autowired
    private TeamService teamService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Test
    void mysqlMigrationsUuidMappingMembershipUpsertsAndOntologyBlobWork() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        UUID memberId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, platform_admin) VALUES (?, ?, ?, FALSE)",
                memberId, "mysql-member@example.test", "test-hash");

        Authentication admin = authenticated(ADMIN_EMAIL);
        UUID adminId = UUID.fromString(jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE email = ?", String.class, ADMIN_EMAIL));
        TeamService.TeamView team = teamService.create("MySQL team", admin);
        teamService.addMemberByEmail(team.id(), "MYSQL-MEMBER@example.test", TeamRole.MEMBER, admin);
        assertThat(teamService.listMembers(team.id(), admin))
                .extracting(TeamService.TeamMemberView::userId)
                .contains(memberId);

        assertThatThrownBy(() -> teamService.addMember(team.id(), adminId, TeamRole.MEMBER, admin))
                .hasMessageContaining("owner role");

        var project = projectService.create("MySQL project", Visibility.PRIVATE, null, admin);
        projectService.setMemberByEmail(project.id(), "mysql-member@example.test", ProjectRole.VIEWER, admin);
        assertThat(projectService.listMembers(project.id(), admin))
                .extracting(com.specomega.openprotege.server.project.ProjectService.ProjectMemberView::userId)
                .contains(memberId);

        UUID versionId = UUID.randomUUID();
        byte[] content = {0, 1, 2, 3, (byte) 255};
        jdbcTemplate.update(
                """
                INSERT INTO ontology_versions
                    (id, project_id, format, content, axiom_count, file_name, created_by)
                VALUES (?, ?, 'Turtle', ?, 0, 'sample.ttl', ?)
                """,
                versionId, project.id(), content, adminId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT content FROM ontology_versions WHERE id = ?", byte[].class, versionId))
                .containsExactly(content);
    }

    @Test
    void mysqlHttpApiSupportsLoginOntologySnapshotsAndReasoning() throws Exception {
        ApiClient admin = new ApiClient();
        Response readiness = admin.get("/actuator/health/readiness");
        assertThat(readiness.status()).isEqualTo(200);
        assertThat(readiness.body()).contains("\"status\":\"UP\"");
        admin.login(ADMIN_EMAIL, ADMIN_PASSWORD);

        Response project = admin.post("/api/projects",
                "{\"name\":\"MySQL API project\",\"visibility\":\"PRIVATE\"}");
        assertThat(project.status()).as(project.body()).isEqualTo(201);
        UUID projectId = UUID.fromString(objectMapper.readTree(project.body()).get("id").asText());

        String ontology = """
                <?xml version="1.0"?>
                <rdf:RDF xmlns="http://example.com/mysql#"
                         xml:base="http://example.com/mysql"
                         xmlns:owl="http://www.w3.org/2002/07/owl#"
                         xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                         xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">
                  <owl:Ontology rdf:about="http://example.com/mysql"/>
                  <owl:Class rdf:about="http://example.com/mysql#Person"/>
                  <owl:Class rdf:about="http://example.com/mysql#Student">
                    <rdfs:subClassOf rdf:resource="http://example.com/mysql#Person"/>
                  </owl:Class>
                  <owl:NamedIndividual rdf:about="http://example.com/mysql#alice">
                    <rdf:type rdf:resource="http://example.com/mysql#Student"/>
                  </owl:NamedIndividual>
                </rdf:RDF>
                """;
        Response imported = admin.postMultipart(
                "/api/projects/" + projectId + "/ontologies/import", "mysql.owl", ontology);
        assertThat(imported.status()).as(imported.body()).isEqualTo(201);
        JsonNode importedVersion = objectMapper.readTree(imported.body());
        UUID versionId = UUID.fromString(importedVersion.get("id").asText());

        Response exported = admin.get("/api/projects/" + projectId
                + "/ontologies/versions/" + versionId + "/export");
        assertThat(exported.status()).isEqualTo(200);
        assertThat(exported.body()).isEqualTo(ontology);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT content FROM ontology_versions WHERE id = ?", byte[].class, versionId))
                .containsExactly(ontology.getBytes(StandardCharsets.UTF_8));

        Response classification = admin.post(
                "/api/projects/" + projectId + "/ontologies/versions/" + versionId + "/reasoning/classify",
                "{\"individualIri\":\"http://example.com/mysql#alice\"}");
        assertThat(classification.status()).as(classification.body()).isEqualTo(200);
        JsonNode types = objectMapper.readTree(classification.body()).get("allTypeIris");
        assertThat(types).extracting(JsonNode::asText)
                .contains("http://example.com/mysql#Student", "http://example.com/mysql#Person");

        Response restored = admin.post(
                "/api/projects/" + projectId + "/ontologies/versions/" + versionId + "/restore", "{}");
        assertThat(restored.status()).as(restored.body()).isEqualTo(201);
        UUID restoredVersionId = UUID.fromString(
                objectMapper.readTree(restored.body()).get("id").asText());
        assertThat(restoredVersionId).isNotEqualTo(versionId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT source_version_id FROM ontology_audit_logs WHERE version_id = ? AND action = 'RESTORE'",
                String.class, restoredVersionId)).isEqualTo(versionId.toString());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT content FROM ontology_versions WHERE id = ?", byte[].class, restoredVersionId))
                .containsExactly(ontology.getBytes(StandardCharsets.UTF_8));
    }

    private static Authentication authenticated(String email) {
        return UsernamePasswordAuthenticationToken.authenticated(
                email, "not-used", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private record Response(int status, String body) {}

    private final class ApiClient {
        private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();
        private String csrf;

        void login(String email, String password) throws Exception {
            refreshCsrf();
            Response response = post("/api/auth/login",
                    "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
            assertThat(response.status()).as(response.body()).isEqualTo(204);
            refreshCsrf();
        }

        Response get(String path) throws Exception {
            return send(HttpRequest.newBuilder(uri(path)).GET().build());
        }

        Response post(String path, String json) throws Exception {
            if (csrf == null) {
                refreshCsrf();
            }
            return send(HttpRequest.newBuilder(uri(path))
                    .header("Content-Type", "application/json")
                    .header("X-XSRF-TOKEN", csrf)
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build());
        }

        Response postMultipart(String path, String fileName, String content) throws Exception {
            if (csrf == null) {
                refreshCsrf();
            }
            String boundary = "OpenProtegeBoundary" + UUID.randomUUID().toString().replace("-", "");
            byte[] body = ("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
                    + "Content-Type: application/octet-stream\r\n\r\n"
                    + content + "\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
            return send(HttpRequest.newBuilder(uri(path))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .header("X-XSRF-TOKEN", csrf)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build());
        }

        private void refreshCsrf() throws Exception {
            Response response = send(HttpRequest.newBuilder(uri("/api/auth/csrf")).GET().build());
            assertThat(response.status()).as(response.body()).isEqualTo(200);
            csrf = objectMapper.readTree(response.body()).get("token").asText();
        }

        private Response send(HttpRequest request) throws Exception {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        }

        private URI uri(String path) {
            return URI.create("http://localhost:" + port + path);
        }
    }
}
