-- ============================================================
-- V14: Seed system roles
-- Uses INSERT IGNORE to be safe on re-run
-- ============================================================

INSERT IGNORE INTO roles(code, name) VALUES
    ('VT-01', 'ADMIN'),
    ('VT-02', 'ORG_MANAGER'),
    ('VT-03', 'EVENT_RECORDER'),
    ('VT-04', 'PROCUREMENT'),
    ('VT-05', 'REGULATOR'),
    ('VT-06', 'CONSUMER');