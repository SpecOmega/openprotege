CREATE TABLE integration_probe (
    probe_id INTEGER PRIMARY KEY,
    value VARCHAR(32) NOT NULL
);

INSERT INTO integration_probe (probe_id, value) VALUES (1, 'ready');
