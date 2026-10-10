package com.specomega.openprotege.server;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OpenProtegeServerApplicationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("openprotege_test")
                    .withUsername("openprotege_test")
                    .withPassword("test-only-password");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.connection-timeout", () -> 1_000);
        registry.add("openprotege.bootstrap.admin-email", () -> "bootstrap@example.test");
        registry.add("openprotege.bootstrap.admin-password", () -> "test-bootstrap-password-123");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @Test
    @Order(1)
    void readinessReportsDatabaseAvailability() {
        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health/readiness",
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @Order(2)
    void permitsWebEntryAndStaticAssetPaths() {
        var indexResponse = restTemplate.getForEntity("http://localhost:" + port + "/", String.class);
        var assetResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/assets/not-present.js", String.class);

        assertThat(indexResponse.getStatusCode().value()).isEqualTo(404);
        assertThat(assetResponse.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @Order(3)
    void flywayAppliesMigrationsAgainstPostgres() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1000");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT value FROM integration_probe WHERE probe_id = 1", String.class))
                .isEqualTo("ready");
    }

    @Test
    @Order(4)
    void readinessReportsDatabaseUnavailability() {
        POSTGRES.stop();

        var response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health/readiness",
                String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).contains("\"status\":\"DOWN\"");
    }
}
