package com.specomega.openprotege.server.ontology;

import com.specomega.openprotege.server.project.ProjectService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OntologyService {
    private final JdbcTemplate jdbcTemplate;
    private final ProjectService projectService;
    private final OntologyParser ontologyParser;
    private final TransactionTemplate transactionTemplate;
    private final long maximumFileSize;

    public OntologyService(JdbcTemplate jdbcTemplate,
                           ProjectService projectService,
                           OntologyParser ontologyParser,
                           PlatformTransactionManager transactionManager,
                           @Value("${openprotege.ontology.max-file-size:500MB}") org.springframework.util.unit.DataSize maximumFileSize) {
        this.jdbcTemplate = jdbcTemplate;
        this.projectService = projectService;
        this.ontologyParser = ontologyParser;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.maximumFileSize = maximumFileSize.toBytes();
    }

    public VersionView importOntology(UUID projectId, MultipartFile upload, String requestedFormat,
                                      Authentication actor) {
        UUID actorId = projectService.requireOntologyWriteAccess(projectId, actor);
        long size = upload.getSize();
        String fileName = safeFileName(upload.getOriginalFilename());
        if (size > maximumFileSize) {
            recordAudit(projectId, null, actorId, "IMPORT", "FAILED", fileName, size, null, "FILE_SIZE_EXCEEDED");
            throw OntologyException.fileSizeExceeded(size, maximumFileSize);
        }

        Path temporaryFile = null;
        OntologyFormat resolvedFormat = null;
        try {
            temporaryFile = Files.createTempFile("openprotege-ontology-", ".upload");
            try (var input = upload.getInputStream()) {
                Files.copy(input, temporaryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            long actualSize = Files.size(temporaryFile);
            if (actualSize > maximumFileSize) {
                recordAudit(projectId, null, actorId, "IMPORT", "FAILED", fileName, actualSize, null,
                        "FILE_SIZE_EXCEEDED");
                throw OntologyException.fileSizeExceeded(actualSize, maximumFileSize);
            }
            OntologyParser.ParsedOntology parsed =
                    ontologyParser.parse(temporaryFile, fileName, requestedFormat);
            resolvedFormat = parsed.format();
            UUID versionId = UUID.randomUUID();
            Instant createdAt = Instant.now();
            OntologyFormat formatForInsert = resolvedFormat;
            Path stagedFile = temporaryFile;
            transactionTemplate.executeWithoutResult(status -> {
                try (var input = Files.newInputStream(stagedFile)) {
                    jdbcTemplate.update(connection -> {
                        PreparedStatement statement = connection.prepareStatement(
                                """
                                INSERT INTO ontology_versions
                                    (id, project_id, format, content, ontology_iri, axiom_count, file_name, created_by)
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                                """);
                        statement.setObject(1, versionId);
                        statement.setObject(2, projectId);
                        statement.setString(3, formatForInsert.displayName());
                        statement.setBinaryStream(4, input, actualSize);
                        statement.setString(5, parsed.ontologyIri());
                        statement.setLong(6, parsed.axiomCount());
                        statement.setString(7, fileName);
                        statement.setObject(8, actorId);
                        return statement;
                    });
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to read staged ontology file", exception);
                }
                recordAudit(projectId, versionId, actorId, "IMPORT", "SUCCESS", fileName, actualSize,
                        formatForInsert, null);
                jdbcTemplate.update("UPDATE projects SET updated_at = CURRENT_TIMESTAMP WHERE id = ?", projectId);
            });
            return new VersionView(versionId, projectId, resolvedFormat.displayName(), parsed.ontologyIri(),
                    parsed.axiomCount(), fileName, createdAt);
        } catch (OntologyException exception) {
            if (!"FILE_SIZE_EXCEEDED".equals(exception.errorCode())) {
                recordAudit(projectId, null, actorId, "IMPORT", "FAILED", fileName, size, resolvedFormat,
                        exception.errorCode());
            }
            throw exception;
        } catch (IOException exception) {
            recordAudit(projectId, null, actorId, "IMPORT", "FAILED", fileName, size, resolvedFormat,
                    "PARSING_ERROR");
            throw OntologyException.parsingFailure(exception);
        } catch (RuntimeException exception) {
            recordAudit(projectId, null, actorId, "IMPORT", "FAILED", fileName, size, resolvedFormat,
                    "STORAGE_ERROR");
            throw OntologyException.storageFailure(exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to remove staged ontology file", exception);
                }
            }
        }
    }

    public ExportFile exportOntology(UUID projectId, UUID versionId, String requestedFormat,
                                     Authentication actor) {
        projectService.get(projectId, actor);
        StoredVersion version = findVersion(projectId, versionId);
        UUID actorId = actorId(actor);
        OntologyFormat targetFormat = version.format();
        try {
            OntologyFormat requested = OntologyFormat.parse(requestedFormat);
            if (requested != null) {
                targetFormat = requested;
            }
            byte[] content = targetFormat == version.format()
                    ? version.content()
                    : ontologyParser.convert(version.content(), version.format(), targetFormat);
            recordAudit(projectId, versionId, actorId, "EXPORT", "SUCCESS", version.fileName(),
                    content.length, targetFormat, null);
            return new ExportFile(content, targetFormat.mediaType(),
                    stripExtension(version.fileName()) + targetFormat.extension());
        } catch (OntologyException exception) {
            recordAudit(projectId, versionId, actorId, "EXPORT", "FAILED", version.fileName(),
                    version.content().length, targetFormat, exception.errorCode());
            throw exception;
        }
    }

    public VersionPage listVersions(UUID projectId, int pageNum, int pageSize, Authentication actor) {
        projectService.get(projectId, actor);
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("pageNum must be positive and pageSize must be between 1 and 100");
        }
        int offset = Math.multiplyExact(pageNum - 1, pageSize);
        List<VersionView> versions = jdbcTemplate.query(
                """
                SELECT id, project_id, format, ontology_iri, axiom_count, file_name, created_at
                FROM ontology_versions
                WHERE project_id = ?
                ORDER BY created_at DESC, id
                LIMIT ? OFFSET ?
                """,
                (rs, row) -> new VersionView(rs.getObject("id", UUID.class),
                        rs.getObject("project_id", UUID.class), rs.getString("format"),
                        rs.getString("ontology_iri"), rs.getLong("axiom_count"), rs.getString("file_name"),
                        rs.getTimestamp("created_at").toInstant()),
                projectId, pageSize, offset);
        long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ontology_versions WHERE project_id = ?", Long.class, projectId);
        return new VersionPage(versions, pageNum, pageSize, total);
    }

    public VersionView getVersion(UUID projectId, UUID versionId, Authentication actor) {
        projectService.get(projectId, actor);
        return findVersionMetadata(projectId, versionId);
    }

    public OntologyDocument getOntologyDocument(UUID projectId, UUID versionId, Authentication actor) {
        projectService.get(projectId, actor);
        StoredVersion version = findVersion(projectId, versionId);
        return new OntologyDocument(version.content(), version.format(), version.id());
    }

    private StoredVersion findVersion(UUID projectId, UUID versionId) {
        return jdbcTemplate.query(
                        """
                        SELECT id, project_id, format, content, ontology_iri, axiom_count, file_name, created_at
                        FROM ontology_versions
                        WHERE project_id = ? AND id = ?
                        """,
                        (rs, row) -> new StoredVersion(rs.getObject("id", UUID.class),
                                rs.getObject("project_id", UUID.class),
                                OntologyFormat.parse(rs.getString("format")),
                                rs.getBytes("content"), rs.getString("ontology_iri"),
                                rs.getLong("axiom_count"), rs.getString("file_name"),
                                rs.getTimestamp("created_at").toInstant()),
                        projectId, versionId)
                .stream().findFirst().orElseThrow(OntologyException::versionNotFound);
    }

    private VersionView findVersionMetadata(UUID projectId, UUID versionId) {
        return jdbcTemplate.query(
                        """
                        SELECT id, project_id, format, ontology_iri, axiom_count, file_name, created_at
                        FROM ontology_versions
                        WHERE project_id = ? AND id = ?
                        """,
                        (rs, row) -> new VersionView(rs.getObject("id", UUID.class),
                                rs.getObject("project_id", UUID.class), rs.getString("format"),
                                rs.getString("ontology_iri"), rs.getLong("axiom_count"),
                                rs.getString("file_name"), rs.getTimestamp("created_at").toInstant()),
                        projectId, versionId)
                .stream().findFirst().orElseThrow(OntologyException::versionNotFound);
    }

    private void recordAudit(UUID projectId, UUID versionId, UUID actorId, String action, String result,
                             String fileName, long fileSize, OntologyFormat format, String errorCode) {
        jdbcTemplate.update(
                """
                INSERT INTO ontology_audit_logs
                    (id, project_id, version_id, actor_id, action, result, file_name,
                     file_size_bytes, format, error_code)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(), projectId, versionId, actorId, action, result, fileName,
                fileSize, format == null ? null : format.displayName(), errorCode);
    }

    private UUID actorId(Authentication actor) {
        if (actor == null || !actor.isAuthenticated()
                || actor instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return null;
        }
        return jdbcTemplate.query(
                        "SELECT id FROM users WHERE email = ?",
                        (rs, row) -> rs.getObject("id", UUID.class), actor.getName())
                .stream().findFirst().orElse(null);
    }

    private static String safeFileName(String original) {
        String normalizedPath = original == null ? "" : original.replace('\\', '/');
        int lastSeparator = normalizedPath.lastIndexOf('/');
        String fileName = normalizedPath.isBlank() ? "ontology" : normalizedPath.substring(lastSeparator + 1);
        String normalized = fileName.replaceAll("[\\p{Cntrl}]", "_");
        return normalized.length() <= 255 ? normalized : normalized.substring(normalized.length() - 255);
    }

    private static String stripExtension(String fileName) {
        int extension = fileName.lastIndexOf('.');
        return extension > 0 ? fileName.substring(0, extension) : fileName;
    }

    public record VersionView(UUID id, UUID projectId, String format, String ontologyIri,
                              long axiomCount, String fileName, Instant createdAt) {}
    public record VersionPage(List<VersionView> versions, int pageNum, int pageSize, long total) {}
    public record ExportFile(byte[] content, String mediaType, String fileName) {}
    public record OntologyDocument(byte[] content, OntologyFormat format, UUID versionId) {}
    private record StoredVersion(UUID id, UUID projectId, OntologyFormat format, byte[] content,
                                 String ontologyIri, long axiomCount, String fileName, Instant createdAt) {}
}
