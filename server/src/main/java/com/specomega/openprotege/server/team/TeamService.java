package com.specomega.openprotege.server.team;

import com.specomega.openprotege.server.security.ActorResolver;
import com.specomega.openprotege.server.security.AuditLog;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class TeamService {
    private final JdbcTemplate jdbcTemplate;
    private final ActorResolver actorResolver;
    private final AuditLog auditLog;

    public TeamService(JdbcTemplate jdbcTemplate, ActorResolver actorResolver, AuditLog auditLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.actorResolver = actorResolver;
        this.auditLog = auditLog;
    }

    @Transactional
    public TeamView create(String name, org.springframework.security.core.Authentication actor) {
        UUID userId = actorResolver.requireUserId(actor);
        UUID teamId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO teams (id, name) VALUES (?, ?)", teamId, name.trim());
        jdbcTemplate.update(
                "INSERT INTO team_memberships (team_id, user_id, role) VALUES (?, ?, 'OWNER')",
                teamId, userId);
        auditLog.record(userId, "TEAM_CREATED", "TEAM", teamId);
        return new TeamView(teamId, name.trim(), TeamRole.OWNER);
    }

    @Transactional
    public void addMember(UUID teamId, UUID targetUserId, TeamRole requestedRole,
                          org.springframework.security.core.Authentication actor) {
        UUID actorId = actorResolver.requireUserId(actor);
        TeamRole actorRole = teamRole(teamId, actorId);
        if (actorRole != TeamRole.OWNER && actorRole != TeamRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Team membership management is not allowed");
        }
        if (requestedRole == TeamRole.OWNER || (actorRole == TeamRole.ADMIN && requestedRole != TeamRole.MEMBER)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The requested team role cannot be assigned");
        }
        ensureUserExists(targetUserId);
        int changed = jdbcTemplate.update(
                """
                INSERT INTO team_memberships (team_id, user_id, role)
                VALUES (?, ?, ?)
                ON CONFLICT (team_id, user_id) DO UPDATE SET role = EXCLUDED.role
                WHERE team_memberships.role <> 'OWNER'
                """,
                teamId, targetUserId, requestedRole.name());
        if (changed != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The team owner role cannot be changed here");
        }
        auditLog.record(actorId, "TEAM_MEMBER_SET", "TEAM", teamId, targetUserId);
    }

    @Transactional
    public void addMemberByEmail(UUID teamId, String email, TeamRole requestedRole,
                                 org.springframework.security.core.Authentication actor) {
        UUID actorId = actorResolver.requireUserId(actor);
        TeamRole actorRole = teamRole(teamId, actorId);
        if (actorRole != TeamRole.OWNER && actorRole != TeamRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Team membership management is not allowed");
        }
        UUID targetUserId = jdbcTemplate.query(
                        "SELECT id FROM users WHERE email = ?",
                        (rs, row) -> rs.getObject("id", UUID.class),
                        email.trim().toLowerCase(java.util.Locale.ROOT))
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        addMember(teamId, targetUserId, requestedRole, actor);
    }

    public java.util.List<TeamView> list(org.springframework.security.core.Authentication actor) {
        UUID userId = actorResolver.requireUserId(actor);
        return jdbcTemplate.query(
                """
                SELECT t.id, t.name, tm.role
                FROM teams t
                JOIN team_memberships tm ON tm.team_id = t.id
                WHERE tm.user_id = ?
                ORDER BY t.name, t.id
                """,
                (rs, row) -> new TeamView(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        TeamRole.valueOf(rs.getString("role"))),
                userId);
    }

    public java.util.List<TeamMemberView> listMembers(
            UUID teamId, org.springframework.security.core.Authentication actor) {
        UUID actorId = actorResolver.requireUserId(actor);
        TeamRole role = teamRole(teamId, actorId);
        if (role != TeamRole.OWNER && role != TeamRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Team member listing is not allowed");
        }
        return jdbcTemplate.query(
                """
                SELECT u.id, u.email, tm.role
                FROM team_memberships tm
                JOIN users u ON u.id = tm.user_id
                WHERE tm.team_id = ?
                ORDER BY u.email
                """,
                (rs, row) -> new TeamMemberView(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        TeamRole.valueOf(rs.getString("role"))),
                teamId);
    }

    private TeamRole teamRole(UUID teamId, UUID userId) {
        return jdbcTemplate.query(
                        "SELECT role FROM team_memberships WHERE team_id = ? AND user_id = ?",
                        (rs, row) -> TeamRole.valueOf(rs.getString("role")),
                        teamId, userId)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }

    private void ensureUserExists(UUID userId) {
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    public enum TeamRole { OWNER, ADMIN, MEMBER }

    public record TeamView(UUID id, String name, TeamRole role) {}
    public record TeamMemberView(UUID userId, String email, TeamRole role) {}
}
