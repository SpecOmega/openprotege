CREATE TABLE ontology_versions (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    project_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    format VARCHAR(16) NOT NULL,
    content LONGBLOB NOT NULL,
    ontology_iri TEXT,
    axiom_count BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    created_by CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ontology_versions_format_check CHECK (format IN ('RDF/XML', 'Turtle')),
    CONSTRAINT ontology_versions_axiom_count_check CHECK (axiom_count >= 0),
    CONSTRAINT ontology_versions_project_fk FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT ontology_versions_created_by_fk FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX ontology_versions_project_created_idx
    ON ontology_versions (project_id, created_at DESC, id);

CREATE TABLE ontology_audit_logs (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    project_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    version_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin,
    actor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin,
    action VARCHAR(16) NOT NULL,
    result VARCHAR(16) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    format VARCHAR(16),
    error_code VARCHAR(64),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT ontology_audit_logs_action_check CHECK (action IN ('IMPORT', 'EXPORT')),
    CONSTRAINT ontology_audit_logs_result_check CHECK (result IN ('SUCCESS', 'FAILED')),
    CONSTRAINT ontology_audit_logs_file_size_check CHECK (file_size_bytes >= 0),
    CONSTRAINT ontology_audit_logs_project_fk FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT ontology_audit_logs_version_fk FOREIGN KEY (version_id) REFERENCES ontology_versions(id) ON DELETE SET NULL,
    CONSTRAINT ontology_audit_logs_actor_fk FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX ontology_audit_logs_project_created_idx
    ON ontology_audit_logs (project_id, created_at DESC);
