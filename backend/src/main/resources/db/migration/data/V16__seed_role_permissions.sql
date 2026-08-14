-- ============================================================
-- V16: Seed role-permission mappings
-- Maps each permission to appropriate roles
-- Uses INSERT IGNORE + deterministic UUIDs via subquery
-- ============================================================

-- ============================================================
-- ADMIN (VT-01, role_id = 1): Full system access
-- ============================================================
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 1, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 1 AND rp.permission_id = p.permission_id
);

-- ============================================================
-- ORG_MANAGER (VT-02, role_id = 2): HTX management
-- ============================================================
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 2, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE (p.resource, p.action) IN (
    ('organization','READ'),
    ('organization','UPDATE'),
    ('farm_area','CREATE'),
    ('farm_area','READ'),
    ('farm_area','UPDATE'),
    ('farm_area','DELETE'),
    ('production_lot','CREATE'),
    ('production_lot','READ'),
    ('production_lot','UPDATE'),
    ('production_lot','APPROVE'),
    ('farm_log','READ'),
    ('farm_log','VERIFY'),
    ('shipment','CREATE'),
    ('shipment','READ'),
    ('shipment','UPDATE'),
    ('shipment','EXPORT'),
    ('trace_code','READ'),
    ('chain_event','READ'),
    ('certification','CREATE'),
    ('certification','READ'),
    ('certification','UPDATE'),
    ('standard','READ'),
    ('product_category','READ'),
    ('organization_user','CREATE'),
    ('organization_user','READ'),
    ('organization_user','UPDATE'),
    ('organization_user','DELETE'),
    ('role_permission','READ'),
    ('role_permission','UPDATE'),
    ('notification','READ'),
    ('alert','READ'),
    ('report','READ'),
    ('report','EXPORT'),
    ('scan_statistics','READ'),
    ('activity_log','READ'),
    ('product_feedback','READ'),
    ('code_range','CREATE'),
    ('code_range','READ'),
    ('code_range','UPDATE'),
    ('traceability','READ'),
    ('recall','CREATE'),
    ('recall','READ'),
    ('export','CREATE'),
    ('export','READ')
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 2 AND rp.permission_id = p.permission_id
);

-- ============================================================
-- EVENT_RECORDER (VT-03, role_id = 3): Field data recording
-- ============================================================
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 3, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE (p.resource, p.action) IN (
    ('farm_area','READ'),
    ('production_lot','READ'),
    ('farm_log','CREATE'),
    ('farm_log','READ'),
    ('farm_log','UPDATE'),
    ('chain_event','CREATE'),
    ('chain_event','READ'),
    ('shipment','READ'),
    ('trace_code','READ'),
    ('notification','READ')
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 3 AND rp.permission_id = p.permission_id
);

-- ============================================================
-- PROCUREMENT (VT-04, role_id = 4): Procurement enterprise
-- ============================================================
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 4, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE (p.resource, p.action) IN (
    ('production_lot','READ'),
    ('farm_log','READ'),
    ('shipment','READ'),
    ('trace_code','READ'),
    ('trace_code','ACTIVATE'),
    ('chain_event','CREATE'),
    ('chain_event','READ'),
    ('traceability','READ'),
    ('notification','READ'),
    ('product_feedback','READ')
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 4 AND rp.permission_id = p.permission_id
);

-- ============================================================
-- REGULATOR (VT-05, role_id = 5): Government regulator
-- ============================================================
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 5, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE (p.resource, p.action) IN (
    ('organization','READ'),
    ('farm_area','READ'),
    ('production_lot','READ'),
    ('farm_log','READ'),
    ('shipment','READ'),
    ('trace_code','READ'),
    ('chain_event','READ'),
    ('certification','READ'),
    ('standard','READ'),
    ('product_category','READ'),
    ('report','READ'),
    ('report','EXPORT'),
    ('scan_statistics','READ'),
    ('activity_log','READ'),
    ('product_feedback','READ'),
    ('traceability','READ'),
    ('recall','READ'),
    ('alert','READ'),
    ('notification','READ')
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 5 AND rp.permission_id = p.permission_id
);

-- ============================================================
-- CONSUMER (VT-06, role_id = 6): End consumer - trace only
-- ============================================================
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, is_enabled, created_at)
SELECT UUID(), 6, p.permission_id, TRUE, NOW()
FROM permissions p
WHERE (p.resource, p.action) IN (
    ('traceability','READ'),
    ('product_feedback','READ')
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = 6 AND rp.permission_id = p.permission_id
);