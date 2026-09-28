-- Ghi lại tổ chức mà người dùng đại diện tại thời điểm tạo ChainEvent.
-- Tránh suy ngược từ membership hiện tại vì một user có thể thuộc nhiều tổ chức.
ALTER TABLE chain_events
    ADD COLUMN recorded_organization_id CHAR(36) NULL AFTER recorded_by;

-- Ưu tiên backfill từ activity log gắn trực tiếp với ChainEvent.
UPDATE chain_events ce
JOIN (
    SELECT entity_id, MIN(organization_id) AS organization_id
    FROM activity_logs
    WHERE entity_type = 'ChainEvent'
      AND entity_id IS NOT NULL
    GROUP BY entity_id
    HAVING COUNT(DISTINCT organization_id) = 1
) al ON al.entity_id = ce.id
SET ce.recorded_organization_id = al.organization_id
WHERE ce.event_type = 'PROCUREMENT'
  AND ce.recorded_organization_id IS NULL;

-- Chỉ dùng membership để backfill khi user có đúng một membership ACTIVE.
UPDATE chain_events ce
JOIN (
    SELECT user_id, MIN(organization_id) AS organization_id
    FROM organization_users
    WHERE status = 'ACTIVE'
    GROUP BY user_id
    HAVING COUNT(*) = 1
) membership ON membership.user_id = ce.recorded_by
SET ce.recorded_organization_id = membership.organization_id
WHERE ce.event_type = 'PROCUREMENT'
  AND ce.recorded_organization_id IS NULL;

ALTER TABLE chain_events
    ADD CONSTRAINT fk_chain_event_recorded_organization
        FOREIGN KEY (recorded_organization_id) REFERENCES organizations(organization_id),
    ADD INDEX idx_chain_event_procurement_org
        (shipment_id, event_type, recorded_organization_id);

-- MySQL cho phép nhiều NULL trong unique index. Cột sinh chỉ có giá trị khi PENDING,
-- do đó mỗi shipment chỉ có tối đa một yêu cầu đang chờ nhưng vẫn giữ được toàn bộ lịch sử.
ALTER TABLE recall_requests
    ADD COLUMN pending_shipment_id CHAR(36)
        GENERATED ALWAYS AS (
            CASE WHEN status = 'PENDING' THEN shipment_id ELSE NULL END
        ) STORED,
    ADD CONSTRAINT uk_recall_request_pending_shipment UNIQUE (pending_shipment_id);
