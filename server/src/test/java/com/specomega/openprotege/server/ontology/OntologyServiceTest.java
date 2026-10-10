package com.specomega.openprotege.server.ontology;

import com.specomega.openprotege.server.project.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.unit.DataSize;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OntologyServiceTest {
    @Test
    void rejectsUploadAboveConfiguredLimitBeforeReadingIt() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProjectService projectService = mock(ProjectService.class);
        UUID actorId = UUID.randomUUID();
        when(projectService.requireOntologyWriteAccess(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(actorId);
        OntologyService service = new OntologyService(jdbcTemplate, projectService, new OntologyParser(),
                mock(PlatformTransactionManager.class), DataSize.ofBytes(10));
        MockMultipartFile upload = new MockMultipartFile(
                "file", "too-large.owl", "application/octet-stream", new byte[11]);

        assertThatThrownBy(() -> service.importOntology(UUID.randomUUID(), upload, null, null))
                .isInstanceOf(OntologyException.class)
                .extracting(exception -> ((OntologyException) exception).errorCode())
                .isEqualTo("FILE_SIZE_EXCEEDED");
    }
}
