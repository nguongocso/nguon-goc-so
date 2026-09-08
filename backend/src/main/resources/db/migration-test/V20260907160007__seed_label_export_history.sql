-- ============================================================
-- V20260907160007: Seed label export history
-- Phase 8: LabelExportHistory (24 bản ghi xuất/in tem)
--
-- Đặc điểm: Idempotent (INSERT IGNORE + Deterministic UUIDs + Session variables)
-- ============================================================

-- ------------------------------------------------------------
-- 0. Nạp lại các biến session SQL
-- ------------------------------------------------------------
SET @htx_org_id      = (SELECT organization_id FROM organizations WHERE code = 'HTX_TEST' LIMIT 1);
SET @user_manager_id = (SELECT user_id FROM users WHERE user_name = 'quanly_htx' LIMIT 1);

SET @shipment_buoi_id    = '00000000-0000-0000-0000-000b00000001';
SET @shipment_xoai_d1_id = '00000000-0000-0000-0000-000b00000002';
SET @shipment_cam_id     = '00000000-0000-0000-0000-000b00000003';
SET @shipment_xoai_d2_id = '00000000-0000-0000-0000-000b00000004';

-- ------------------------------------------------------------
-- 1. LabelExportHistory: 24 lượt xuất in tem cho 4 lô hàng
-- ------------------------------------------------------------
INSERT IGNORE INTO label_export_history (
    id, shipment_id, exported_by, organization_id, exported_at, start_index, end_index, quantity, label_size
) VALUES
('00000000-0000-0000-0000-001200000001', @shipment_buoi_id, @user_manager_id, @htx_org_id, '2026-07-23 09:00:00', 1, 5, 5, '40x30'),
('00000000-0000-0000-0000-001200000002', @shipment_buoi_id, @user_manager_id, @htx_org_id, '2026-07-23 09:30:00', 6, 10, 5, '40x30'),
('00000000-0000-0000-0000-001200000003', @shipment_buoi_id, @user_manager_id, @htx_org_id, '2026-07-23 10:00:00', 11, 15, 5, '50x30'),
('00000000-0000-0000-0000-001200000004', @shipment_buoi_id, @user_manager_id, @htx_org_id, '2026-07-23 10:30:00', 16, 20, 5, '50x30'),
('00000000-0000-0000-0000-001200000005', @shipment_buoi_id, @user_manager_id, @htx_org_id, '2026-07-23 11:00:00', 21, 25, 5, '40x30'),
('00000000-0000-0000-0000-001200000006', @shipment_buoi_id, @user_manager_id, @htx_org_id, '2026-07-23 11:30:00', 26, 30, 5, '30x20'),
('00000000-0000-0000-0000-001200000007', @shipment_xoai_d1_id, @user_manager_id, @htx_org_id, '2026-08-06 08:30:00', 1, 5, 5, '40x30'),
('00000000-0000-0000-0000-001200000008', @shipment_xoai_d1_id, @user_manager_id, @htx_org_id, '2026-08-06 09:00:00', 6, 10, 5, '40x30'),
('00000000-0000-0000-0000-001200000009', @shipment_xoai_d1_id, @user_manager_id, @htx_org_id, '2026-08-06 09:30:00', 11, 15, 5, '40x30'),
('00000000-0000-0000-0000-001200000010', @shipment_xoai_d1_id, @user_manager_id, @htx_org_id, '2026-08-06 10:00:00', 16, 20, 5, '50x30'),
('00000000-0000-0000-0000-001200000011', @shipment_xoai_d1_id, @user_manager_id, @htx_org_id, '2026-08-06 10:30:00', 21, 25, 5, '50x30'),
('00000000-0000-0000-0000-001200000012', @shipment_xoai_d1_id, @user_manager_id, @htx_org_id, '2026-08-06 11:00:00', 26, 30, 5, '50x30'),
('00000000-0000-0000-0000-001200000013', @shipment_xoai_d1_id, @user_manager_id, @htx_org_id, '2026-08-06 13:30:00', 31, 35, 5, '30x20'),
('00000000-0000-0000-0000-001200000014', @shipment_xoai_d1_id, @user_manager_id, @htx_org_id, '2026-08-06 14:00:00', 36, 40, 5, '30x20'),
('00000000-0000-0000-0000-001200000015', @shipment_cam_id, @user_manager_id, @htx_org_id, '2026-06-20 09:00:00', 1, 4, 4, '40x30'),
('00000000-0000-0000-0000-001200000016', @shipment_cam_id, @user_manager_id, @htx_org_id, '2026-06-20 09:30:00', 5, 8, 4, '40x30'),
('00000000-0000-0000-0000-001200000017', @shipment_cam_id, @user_manager_id, @htx_org_id, '2026-06-20 10:00:00', 9, 12, 4, '40x30'),
('00000000-0000-0000-0000-001200000018', @shipment_cam_id, @user_manager_id, @htx_org_id, '2026-06-20 10:30:00', 13, 16, 4, '50x30'),
('00000000-0000-0000-0000-001200000019', @shipment_cam_id, @user_manager_id, @htx_org_id, '2026-06-20 11:00:00', 17, 20, 4, '50x30'),
('00000000-0000-0000-0000-001200000020', @shipment_xoai_d2_id, @user_manager_id, @htx_org_id, '2026-08-11 09:00:00', 1, 5, 5, '40x30'),
('00000000-0000-0000-0000-001200000021', @shipment_xoai_d2_id, @user_manager_id, @htx_org_id, '2026-08-11 09:30:00', 6, 10, 5, '40x30'),
('00000000-0000-0000-0000-001200000022', @shipment_xoai_d2_id, @user_manager_id, @htx_org_id, '2026-08-11 10:00:00', 11, 15, 5, '50x30'),
('00000000-0000-0000-0000-001200000023', @shipment_xoai_d2_id, @user_manager_id, @htx_org_id, '2026-08-11 10:30:00', 16, 20, 5, '50x30'),
('00000000-0000-0000-0000-001200000024', @shipment_xoai_d2_id, @user_manager_id, @htx_org_id, '2026-08-11 11:00:00', 21, 25, 5, '30x20');
