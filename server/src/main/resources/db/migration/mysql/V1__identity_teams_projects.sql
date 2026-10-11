CREATE TABLE users (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    platform_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE user_invitations (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at DATETIME(6) NOT NULL,
    accepted_at DATETIME(6),
    invited_by CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT user_invitations_invited_by_fk FOREIGN KEY (invited_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE INDEX user_invitations_pending_email_idx
    ON user_invitations (email, expires_at, accepted_at);

CREATE TABLE teams (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE TABLE team_memberships (
    team_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `role` VARCHAR(16) NOT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (team_id, user_id),
    CONSTRAINT team_memberships_role_check CHECK (`role` IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT team_memberships_team_fk FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT team_memberships_user_fk FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE projects (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    visibility VARCHAR(8) NOT NULL,
    team_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin,
    created_by CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT projects_visibility_check CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    CONSTRAINT projects_team_fk FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT projects_created_by_fk FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE INDEX projects_team_idx ON projects (team_id);

CREATE TABLE project_memberships (
    project_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    `role` VARCHAR(16) NOT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (project_id, user_id),
    CONSTRAINT project_memberships_role_check CHECK (`role` IN ('OWNER', 'ADMIN', 'EDITOR', 'VIEWER')),
    CONSTRAINT project_memberships_project_fk FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT project_memberships_user_fk FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE audit_events (
    id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,
    actor_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin,
    event_type VARCHAR(64) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT audit_events_actor_fk FOREIGN KEY (actor_id) REFERENCES users(id),
    CONSTRAINT audit_events_subject_fk FOREIGN KEY (subject_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE INDEX audit_events_target_idx ON audit_events (target_type, target_id, created_at);
