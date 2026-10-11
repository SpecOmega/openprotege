package com.specomega.openprotege.server.security;

import com.specomega.openprotege.server.database.JdbcUuid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Component
public class ActorResolver {
    private final JdbcTemplate jdbcTemplate;

    public ActorResolver(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID requireUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return jdbcTemplate.query(
                        "SELECT id FROM users WHERE email = ?",
                        (resultSet, rowNumber) -> JdbcUuid.read(resultSet, "id"),
                        authentication.getName())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
