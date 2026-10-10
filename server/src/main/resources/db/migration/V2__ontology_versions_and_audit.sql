CREATE TABLE ontology_versions (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    format VARCHAR(16) NOT NULL CHECK (format IN ('RDF/XML', 'Turtle')),
    content BYTEA NOT NULL,
    ontology_iri TEXT,
    axiom_count BIGINT NOT NULL CHECK (axiom_count >= 0),
    file_name VARCHAR(255) NOT NULL,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ontology_versions_project_created_idx
    ON ontology_versions (project_id, created_at DESC, id);

CREATE TABLE ontology_audit_logs (
    id UUID PRIMARY KEY,
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    version_id UUID REFERENCES ontology_versions(id) ON DELETE SET NULL,
    actor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(16) NOT NULL CHECK (action IN ('IMPORT', 'EXPORT')),
    result VARCHAR(16) NOT NULL CHECK (result IN ('SUCCESS', 'FAILED')),
    file_name VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL CHECK (file_size_bytes >= 0),
    format VARCHAR(16),
    error_code VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ontology_audit_logs_project_created_idx
    ON ontology_audit_logs (project_id, created_at DESC);
