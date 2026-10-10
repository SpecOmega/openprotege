package com.specomega.openprotege.server.project;

import com.specomega.openprotege.server.security.ActorResolver;
import com.specomega.openprotege.server.security.AuditLog;
import com.specomega.openprotege.server.team.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ProjectService {
    private final JdbcTemplate jdbcTemplate;
    private final ActorResolver actorResolver;
    private final AuditLog auditLog;

    public ProjectService(JdbcTemplate jdbcTemplate, ActorResolver actorResolver, AuditLog auditLog) {
        this.jdbcTemplate = jdbcTemplate;
        this.actorResolver = actorResolver;
        this.auditLog = auditLog;
    }

    @Transactional
    public ProjectView create(String name, Visibility visibility, UUID teamId, Authentication actor) {
        UUID actorId = actorResolver.requireUserId(actor);
        if (teamId != null) {
            TeamService.TeamRole role = teamRole(teamId, actorId);
            if (role != TeamService.TeamRole.OWNER && role != TeamService.TeamRole.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only team owners or admins may create team projects");
            }
        }
        UUID projectId = UUID.randomUUID();
        String normalizedName = name.trim();
        jdbcTemplate.update(
                """
                INSERT INTO projects (id, name, visibility, team_id, created_by)
                VALUES (?, ?, ?, ?, ?)
                """,
                projectId, normalizedName, visibility.name(), teamId, actorId);
        jdbcTemplate.update(
                """
                INSERT INTO project_memberships (project_id, user_id, role)
                VALUES (?, ?, 'OWNER')
                """,
                projectId, actorId);
        auditLog.record(actorId, "PROJECT_CREATED", "PROJECT", projectId);
        return new ProjectView(projectId, normalizedName, visibility, teamId, ProjectRole.OWNER);
    }

    public ProjectView get(UUID projectId, Authentication actor) {
        ProjectRow project = findProject(projectId);
        if (project.visibility() == Visibility.PRIVATE && !isProjectMember(project, actor)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        }
        ProjectRole role = authenticatedUserId(actor)
                .flatMap(userId -> activeProjectRole(project, userId))
                .orElse(null);
        return new ProjectView(project.id(), project.name(), project.visibility(), project.teamId(), role);
    }

    public UUID requireOntologyWriteAccess(UUID projectId, Authentication actor) {
        UUID actorId = actorResolver.requireUserId(actor);
        ProjectRow project = findProject(projectId);
        ProjectRole role = activeProjectRole(project, actorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Ontology import is not allowed"));
        if (role != ProjectRole.OWNER && role != ProjectRole.ADMIN && role != ProjectRole.EDITOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ontology import is not allowed");
        }
        return actorId;
    }

    public java.util.List<ProjectView> list(Authentication actor) {
        UUID actorId = actorResolver.requireUserId(actor);
        return jdbcTemplate.query(
                """
                SELECT p.id, p.name, p.visibility, p.team_id, pm.role
                FROM projects p
                JOIN project_memberships pm ON pm.project_id = p.id
                WHERE pm.user_id = ?
                  AND (p.team_id IS NULL OR EXISTS (
                      SELECT 1 FROM team_memberships tm
                      WHERE tm.team_id = p.team_id AND tm.user_id = pm.user_id
                  ))
                ORDER BY p.updated_at DESC, p.id
                """,
                (rs, row) -> new ProjectView(
                        rs.getObject("id", UUID.class),
                        rs.getString("name"),
                        Visibility.valueOf(rs.getString("visibility")),
                        rs.getObject("team_id", UUID.class),
                        ProjectRole.valueOf(rs.getString("role"))),
                actorId);
    }

    @Transactional
    public ProjectView update(UUID projectId, String name, Visibility visibility, Authentication actor) {
        UUID actorId = actorResolver.requireUserId(actor);
        ProjectRow project = findProject(projectId);
        requireProjectManager(project, actorId);
        String normalizedName = name.trim();
        jdbcTemplate.update(
                """
                UPDATE projects SET name = ?, visibility = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                normalizedName, visibility.name(), projectId);
        auditLog.record(actorId, "PROJECT_UPDATED", "PROJECT", projectId);
        return new ProjectView(projectId, normalizedName, visibility, project.teamId(),
                projectRole(projectId, actorId).orElseThrow());
    }

    @Transactional
    public void setMember(UUID projectId, UUID targetUserId, ProjectRole requestedRole, Authentication actor) {
        UUID actorId = actorResolver.requireUserId(actor);
        ProjectRow project = findProject(projectId);
        requireProjectManager(project, actorId);
        if (requestedRole == ProjectRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project ownership cannot be assigned here");
        }
        if (project.teamId() != null && !isTeamMember(project.teamId(), targetUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A team project member must first belong to its team");
        }
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, targetUserId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        int changed = jdbcTemplate.update(
                """
                INSERT INTO project_memberships (project_id, user_id, role)
                VALUES (?, ?, ?)
                ON CONFLICT (project_id, user_id) DO UPDATE SET role = EXCLUDED.role
                WHERE project_memberships.role <> 'OWNER'
                """,
                projectId, targetUserId, requestedRole.name());
        if (changed != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The project owner role cannot be changed here");
        }
        auditLog.record(actorId, "PROJECT_MEMBER_SET", "PROJECT", projectId, targetUserId);
    }

    public java.util.List<ProjectMemberView> listMembers(UUID projectId, Authentication actor) {
        UUID actorId = actorResolver.requireUserId(actor);
        ProjectRow project = findProject(projectId);
        requireProjectManager(project, actorId);
        return jdbcTemplate.query(
                """
                SELECT u.id, u.email, pm.role
                FROM project_memberships pm
                JOIN users u ON u.id = pm.user_id
                WHERE pm.project_id = ?
                ORDER BY u.email
                """,
                (rs, row) -> new ProjectMemberView(
                        rs.getObject("id", UUID.class),
                        rs.getString("email"),
                        ProjectRole.valueOf(rs.getString("role"))),
                projectId);
    }

    private boolean isProjectMember(ProjectRow project, Authentication actor) {
        return authenticatedUserId(actor)
                .flatMap(userId -> activeProjectRole(project, userId))
                .isPresent();
    }

    private java.util.Optional<ProjectRole> activeProjectRole(ProjectRow project, UUID userId) {
        if (project.teamId() != null && !isTeamMember(project.teamId(), userId)) {
            return java.util.Optional.empty();
        }
        return projectRole(project.id(), userId);
    }

    private void requireProjectManager(ProjectRow project, UUID actorId) {
        ProjectRole role = projectRole(project.id(), actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Project management is not allowed"));
        if (project.teamId() != null && !isTeamMember(project.teamId(), actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current team membership is required");
        }
        if (role != ProjectRole.OWNER && role != ProjectRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Project management is not allowed");
        }
    }

    private ProjectRow findProject(UUID projectId) {
        return jdbcTemplate.query(
                        """
                        SELECT id, name, visibility, team_id
                        FROM projects WHERE id = ?
                        """,
                        (rs, row) -> new ProjectRow(
                                rs.getObject("id", UUID.class),
                                rs.getString("name"),
                                Visibility.valueOf(rs.getString("visibility")),
                                rs.getObject("team_id", UUID.class)),
                        projectId)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    private java.util.Optional<ProjectRole> projectRole(UUID projectId, UUID userId) {
        return jdbcTemplate.query(
                        "SELECT role FROM project_memberships WHERE project_id = ? AND user_id = ?",
                        (rs, row) -> ProjectRole.valueOf(rs.getString("role")),
                        projectId, userId)
                .stream().findFirst();
    }

    private TeamService.TeamRole teamRole(UUID teamId, UUID userId) {
        return jdbcTemplate.query(
                        "SELECT role FROM team_memberships WHERE team_id = ? AND user_id = ?",
                        (rs, row) -> TeamService.TeamRole.valueOf(rs.getString("role")),
                        teamId, userId)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team not found"));
    }

    private boolean isTeamMember(UUID teamId, UUID userId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM team_memberships WHERE team_id = ? AND user_id = ?",
                Integer.class, teamId, userId) > 0;
    }

    private java.util.Optional<UUID> authenticatedUserId(Authentication actor) {
        if (actor == null || !actor.isAuthenticated() || actor instanceof AnonymousAuthenticationToken) {
            return java.util.Optional.empty();
        }
        return jdbcTemplate.query(
                        "SELECT id FROM users WHERE email = ?",
                        (rs, row) -> rs.getObject("id", UUID.class),
                        actor.getName())
                .stream().findFirst();
    }

    public enum Visibility { PUBLIC, PRIVATE }
    public enum ProjectRole { OWNER, ADMIN, EDITOR, VIEWER }

    public record ProjectView(UUID id, String name, Visibility visibility, UUID teamId, ProjectRole role) {}

    public record ProjectMemberView(UUID userId, String email, ProjectRole role) {}

    private record ProjectRow(UUID id, String name, Visibility visibility, UUID teamId) {}
}
