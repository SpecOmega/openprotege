package com.specomega.openprotege.server.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuditLog {
    private final JdbcTemplate jdbcTemplate;

    public AuditLog(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void record(UUID actorId, String eventType, String targetType, UUID targetId) {
        record(actorId, eventType, targetType, targetId, null);
    }

    public void record(UUID actorId, String eventType, String targetType, UUID targetId, UUID subjectId) {
        jdbcTemplate.update(
                """
                INSERT INTO audit_events (id, actor_id, event_type, target_type, target_id, subject_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), actorId, eventType, targetType, targetId, subjectId);
    }
}
