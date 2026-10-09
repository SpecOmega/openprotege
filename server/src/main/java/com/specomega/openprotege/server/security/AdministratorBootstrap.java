package com.specomega.openprotege.server.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Component
public class AdministratorBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdministratorBootstrap(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${openprotege.bootstrap.admin-email:}") String email,
            @Value("${openprotege.bootstrap.admin-password:}") String password) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.password = password == null ? "" : password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (userCount == null || userCount > 0) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "An empty user database requires explicit administrator bootstrap credentials");
        }
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 320) {
            throw new IllegalStateException("Administrator bootstrap email is invalid");
        }
        if (password.length() < 12 || password.length() > 128) {
            throw new IllegalStateException(
                    "Administrator bootstrap password must contain between 12 and 128 characters");
        }
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, platform_admin)
                VALUES (?, ?, ?, TRUE)
                """,
                UUID.randomUUID(), email, passwordEncoder.encode(password));
    }
}
