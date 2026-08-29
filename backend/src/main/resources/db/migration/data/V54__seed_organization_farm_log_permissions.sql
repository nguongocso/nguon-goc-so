-- Seed organization_role_permissions for FARM_LOG and farm_log permissions (VT-02 and VT-03 across all organizations)

INSERT IGNORE INTO organization_role_permissions (id, organization_id, role_id, permission_id, is_enabled, updated_at, updated_by)
SELECT 
    UUID(),
    o.organization_id,
    r.role_id,
    p.permission_id,
    TRUE,
    NOW(),
    NULL
FROM organizations o
CROSS JOIN roles r
CROSS JOIN permissions p
WHERE r.code IN ('VT-02', 'VT-03')
AND p.resource IN ('FARM_LOG', 'farm_log')
AND p.action IN ('CREATE', 'READ', 'UPDATE', 'VERIFY')
AND NOT EXISTS (
    SELECT 1 FROM organization_role_permissions orp
    WHERE orp.organization_id = o.organization_id
    AND orp.role_id = r.role_id
    AND orp.permission_id = p.permission_id
);
