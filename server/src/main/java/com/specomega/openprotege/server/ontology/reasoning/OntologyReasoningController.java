package com.specomega.openprotege.server.ontology.reasoning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/projects/{projectId}/ontologies/versions/{versionId}/reasoning")
public class OntologyReasoningController {
    private final OntologyReasoningService reasoningService;

    public OntologyReasoningController(OntologyReasoningService reasoningService) {
        this.reasoningService = reasoningService;
    }

    @PostMapping("/start")
    public OntologyReasoningService.ValidationResult start(@PathVariable UUID projectId,
                                                           @PathVariable UUID versionId,
                                                           @Valid @RequestBody StartRequest request,
                                                           Authentication actor) {
        return reasoningService.start(projectId, versionId, request.engine(), actor);
    }

    @PostMapping("/validate")
    public OntologyReasoningService.ValidationResult validate(@PathVariable UUID projectId,
                                                              @PathVariable UUID versionId,
                                                              Authentication actor) {
        return reasoningService.validate(projectId, versionId, actor);
    }

    @PostMapping("/hierarchy")
    public OntologyReasoningService.ClassHierarchy hierarchy(@PathVariable UUID projectId,
                                                             @PathVariable UUID versionId,
                                                             @Valid @RequestBody HierarchyRequest request,
                                                             Authentication actor) {
        return reasoningService.hierarchy(projectId, versionId, request.classIri(), request.direct(), actor);
    }

    @PostMapping("/classify")
    public OntologyReasoningService.IndividualClassification classify(@PathVariable UUID projectId,
                                                                      @PathVariable UUID versionId,
                                                                      @Valid @RequestBody IndividualRequest request,
                                                                      Authentication actor) {
        return reasoningService.classify(projectId, versionId, request.individualIri(), actor);
    }

    @PostMapping("/rules/apply")
    public OntologyReasoningService.RuleApplication applyRules(@PathVariable UUID projectId,
                                                               @PathVariable UUID versionId,
                                                               @Valid @RequestBody RuleRequest request,
                                                               Authentication actor) {
        return reasoningService.applyRule(projectId, versionId, request.ruleText(),
                request.individualIri(), actor);
    }

    public record StartRequest(@NotBlank @Size(max = 32) String engine) {}
    public record HierarchyRequest(@NotBlank @Size(max = 2048) String classIri, boolean direct) {}
    public record IndividualRequest(@NotBlank @Size(max = 2048) String individualIri) {}
    public record RuleRequest(@NotBlank @Size(max = 10_000) String ruleText,
                              @NotBlank @Size(max = 2048) String individualIri) {}
}
