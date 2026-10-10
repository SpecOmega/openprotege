package com.specomega.openprotege.server.ontology;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/projects/{projectId}/ontologies")
public class OntologyController {
    private final OntologyService ontologyService;

    public OntologyController(OntologyService ontologyService) {
        this.ontologyService = ontologyService;
    }

    @PostMapping(path = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<OntologyService.VersionView> importOntology(
            @PathVariable UUID projectId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String format,
            Authentication actor) {
        OntologyService.VersionView version = ontologyService.importOntology(projectId, file, format, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(version);
    }

    @GetMapping("/versions")
    OntologyService.VersionPage listVersions(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "1") @Min(1) int pageNum,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            Authentication actor) {
        return ontologyService.listVersions(projectId, pageNum, pageSize, actor);
    }

    @GetMapping("/versions/{versionId}")
    OntologyService.VersionView getVersion(@PathVariable UUID projectId, @PathVariable UUID versionId,
                                           Authentication actor) {
        return ontologyService.getVersion(projectId, versionId, actor);
    }

    @GetMapping("/versions/{versionId}/export")
    ResponseEntity<ByteArrayResource> exportOntology(@PathVariable UUID projectId,
                                                      @PathVariable UUID versionId,
                                                      @RequestParam(required = false) String format,
                                                      Authentication actor) {
        OntologyService.ExportFile file = ontologyService.exportOntology(projectId, versionId, format, actor);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build()
                                .toString())
                .body(new ByteArrayResource(file.content()));
    }
}
