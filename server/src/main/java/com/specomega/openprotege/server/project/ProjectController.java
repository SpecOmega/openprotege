package com.specomega.openprotege.server.project;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    org.springframework.http.ResponseEntity<ProjectService.ProjectView> create(
            @Valid @RequestBody CreateProjectRequest request, Authentication authentication) {
        ProjectService.Visibility visibility = parseVisibility(request.visibility());
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED)
                .body(projectService.create(request.name(), visibility, request.teamId(), authentication));
    }

    @GetMapping("/{projectId}")
    ProjectService.ProjectView get(@PathVariable UUID projectId, Authentication authentication) {
        return projectService.get(projectId, authentication);
    }

    @GetMapping
    java.util.List<ProjectService.ProjectView> list(Authentication authentication) {
        return projectService.list(authentication);
    }

    @GetMapping("/{projectId}/members")
    java.util.List<ProjectService.ProjectMemberView> members(
            @PathVariable UUID projectId, Authentication authentication) {
        return projectService.listMembers(projectId, authentication);
    }

    @PutMapping("/{projectId}")
    ProjectService.ProjectView update(@PathVariable UUID projectId,
                                      @Valid @RequestBody UpdateProjectRequest request,
                                      Authentication authentication) {
        return projectService.update(projectId, request.name(), parseVisibility(request.visibility()), authentication);
    }

    @PostMapping("/{projectId}/members")
    org.springframework.http.ResponseEntity<Void> setMember(
            @PathVariable UUID projectId,
            @Valid @RequestBody SetProjectMemberRequest request,
            Authentication authentication) {
        ProjectService.ProjectRole role;
        try {
            role = ProjectService.ProjectRole.valueOf(request.role());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported project role");
        }
        if (request.userId() == null && request.email() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An account ID or email is required");
        }
        if (request.userId() != null) {
            projectService.setMember(projectId, request.userId(), role, authentication);
        } else {
            projectService.setMemberByEmail(projectId, request.email(), role, authentication);
        }
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static ProjectService.Visibility parseVisibility(String visibility) {
        try {
            return ProjectService.Visibility.valueOf(visibility);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported project visibility");
        }
    }

    record CreateProjectRequest(@NotBlank @Size(max = 160) String name,
                                @NotBlank @Pattern(regexp = "PUBLIC|PRIVATE") String visibility,
                                UUID teamId) {}

    record UpdateProjectRequest(@NotBlank @Size(max = 160) String name,
                                @NotBlank @Pattern(regexp = "PUBLIC|PRIVATE") String visibility) {}

    record SetProjectMemberRequest(UUID userId,
                                   @Email @Size(max = 320) String email,
                                   @NotBlank @Pattern(regexp = "ADMIN|EDITOR|VIEWER") String role) {}
}
