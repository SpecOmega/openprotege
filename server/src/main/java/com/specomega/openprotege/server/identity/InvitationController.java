package com.specomega.openprotege.server.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class InvitationController {
    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @PostMapping("/admin/invitations")
    ResponseEntity<InvitationService.InvitationIssued> issue(
            @Valid @RequestBody IssueInvitationRequest request, Authentication authentication) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(invitationService.issue(request.email(), authentication));
    }

    @PostMapping("/auth/invitations/accept")
    org.springframework.http.ResponseEntity<Void> accept(@Valid @RequestBody AcceptInvitationRequest request) {
        invitationService.accept(request.token(), request.password());
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).build();
    }

    record IssueInvitationRequest(@NotBlank @Email @Size(max = 320) String email) {}

    record AcceptInvitationRequest(@NotBlank @Size(max = 100) String token,
                                   @NotBlank @Size(min = 12, max = 128) String password) {}
}
