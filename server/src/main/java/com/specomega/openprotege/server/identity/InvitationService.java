package com.specomega.openprotege.server.identity;

import com.specomega.openprotege.server.security.ActorResolver;
import com.specomega.openprotege.server.security.AuditLog;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvitationService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ActorResolver actorResolver;
    private final AuditLog auditLog;
    private final SecureRandom secureRandom = new SecureRandom();

    public InvitationService(JdbcTemplate jdbcTemplate,
                             PasswordEncoder passwordEncoder,
                             ActorResolver actorResolver,
                             AuditLog auditLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.actorResolver = actorResolver;
        this.auditLog = auditLog;
    }

    @Transactional
    public InvitationIssued issue(String rawEmail, org.springframework.security.core.Authentication actor) {
        String email = normalizeEmail(rawEmail);
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email");
        }
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        UUID invitationId = UUID.randomUUID();
        UUID actorId = actorResolver.requireUserId(actor);
        jdbcTemplate.update(
                """
                INSERT INTO user_invitations (id, email, token_hash, expires_at, invited_by)
                VALUES (?, ?, ?, ?, ?)
                """,
                invitationId, email, sha256(token), Timestamp.from(expiresAt), actorId);
        auditLog.record(actorId, "USER_INVITED", "INVITATION", invitationId);
        return new InvitationIssued(invitationId, token, expiresAt);
    }

    @Transactional
    public void accept(String token, String password) {
        String digest = sha256(token);
        var invitation = jdbcTemplate.query(
                """
                SELECT id, email, expires_at, accepted_at
                FROM user_invitations
                WHERE token_hash = ?
                FOR UPDATE
                """,
                (resultSet, rowNumber) -> new Invitation(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getTimestamp("expires_at").toInstant(),
                        resultSet.getTimestamp("accepted_at") == null
                                ? null : resultSet.getTimestamp("accepted_at").toInstant()),
                digest).stream().findFirst();
        if (invitation.isEmpty()
                || invitation.get().acceptedAt() != null
                || !invitation.get().expiresAt().isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is invalid or expired");
        }
        if (jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                invitation.get().email()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account already exists for this email");
        }
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO users (id, email, password_hash, platform_admin)
                VALUES (?, ?, ?, FALSE)
                """,
                userId, invitation.get().email(), passwordEncoder.encode(password));
        int updated = jdbcTemplate.update(
                """
                UPDATE user_invitations SET accepted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND accepted_at IS NULL AND expires_at > CURRENT_TIMESTAMP
                """,
                invitation.get().id());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation is invalid or expired");
        }
        auditLog.record(userId, "USER_CREATED", "USER", userId);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record InvitationIssued(UUID id, String token, Instant expiresAt) {}

    private record Invitation(UUID id, String email, Instant expiresAt, Instant acceptedAt) {}
}
