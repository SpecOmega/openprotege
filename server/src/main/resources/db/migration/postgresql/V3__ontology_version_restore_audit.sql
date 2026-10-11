ALTER TABLE ontology_audit_logs
    DROP CONSTRAINT ontology_audit_logs_action_check;

ALTER TABLE ontology_audit_logs
    ADD CONSTRAINT ontology_audit_logs_action_check
    CHECK (action IN ('IMPORT', 'EXPORT', 'RESTORE'));

ALTER TABLE ontology_audit_logs
    ADD COLUMN source_version_id UUID REFERENCES ontology_versions(id) ON DELETE SET NULL;
