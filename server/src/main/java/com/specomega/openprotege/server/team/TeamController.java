package com.specomega.openprotege.server.team;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
public class TeamController {
    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    org.springframework.http.ResponseEntity<TeamService.TeamView> create(
            @Valid @RequestBody CreateTeamRequest request, Authentication authentication) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.create(request.name(), authentication));
    }

    @GetMapping
    java.util.List<TeamService.TeamView> list(Authentication authentication) {
        return teamService.list(authentication);
    }

    @GetMapping("/{teamId}/members")
    java.util.List<TeamService.TeamMemberView> members(
            @PathVariable UUID teamId, Authentication authentication) {
        return teamService.listMembers(teamId, authentication);
    }

    @PostMapping("/{teamId}/members")
    org.springframework.http.ResponseEntity<Void> addMember(
            @PathVariable UUID teamId,
            @Valid @RequestBody AddTeamMemberRequest request,
            Authentication authentication) {
        try {
            teamService.addMember(teamId, request.userId(), TeamService.TeamRole.valueOf(request.role()), authentication);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported team role");
        }
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).build();
    }

    record CreateTeamRequest(@NotBlank @Size(max = 120) String name) {}

    record AddTeamMemberRequest(@NotNull UUID userId,
                                @NotBlank @Pattern(regexp = "ADMIN|MEMBER") String role) {}
}
