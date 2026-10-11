ALTER TABLE ontology_audit_logs
    DROP CHECK ontology_audit_logs_action_check;

ALTER TABLE ontology_audit_logs
    ADD CONSTRAINT ontology_audit_logs_action_check
    CHECK (action IN ('IMPORT', 'EXPORT', 'RESTORE'));

ALTER TABLE ontology_audit_logs
    ADD COLUMN source_version_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin,
    ADD CONSTRAINT ontology_audit_logs_source_version_fk
        FOREIGN KEY (source_version_id) REFERENCES ontology_versions(id) ON DELETE SET NULL;
