-- ============================================================
-- V20260907160006: Seed recalls and inspections
-- Phase 7: RecallRequest (3 yêu cầu) & InspectionRequest (2 yêu cầu)
--
-- Đặc điểm: Idempotent (INSERT IGNORE + Deterministic UUIDs + Session variables)
-- ============================================================

-- ------------------------------------------------------------
-- 0. Nạp lại các biến session SQL
-- ------------------------------------------------------------
SET @user_manager_id  = (SELECT user_id FROM users WHERE user_name = 'quanly_htx' LIMIT 1);
SET @user_recorder_id = (SELECT user_id FROM users WHERE user_name = 'nguoighi' LIMIT 1);

SET @lot_buoi_thu_id  = (SELECT id FROM production_lot WHERE name = 'Lô Bưởi Thu 2026' LIMIT 1);
SET @lot_xoai_he_id   = (SELECT id FROM production_lot WHERE name = 'Lô Xoài Hè 2026' LIMIT 1);
SET @lot_cam_dong_id  = (SELECT id FROM production_lot WHERE name = 'Lô Cam Đông 2026' LIMIT 1);

-- ------------------------------------------------------------
-- 1. RecallRequest: 3 yêu cầu thu hồi (2 APPROVED, 1 REJECTED)
-- ------------------------------------------------------------
INSERT IGNORE INTO recall_requests (
    id, production_lot_id, requested_by, requested_at, reason, evidence, status,
    approved_by, approved_at, approval_remarks,
    rejected_by, rejected_at, rejection_reason,
    created_at, updated_at
) VALUES
(
    '00000000-0000-0000-0000-000e00000001',
    @lot_cam_dong_id,
    @user_recorder_id,
    '2026-06-25 09:00:00',
    'Phát hiện dư lượng thuốc BVTV vượt ngưỡng cho phép sau khi nhận kết quả kiểm nghiệm',
    'Phiếu kết quả thử nghiệm số PKN-2026-CAM-001 của Trung tâm 3',
    'APPROVED',
    @user_manager_id,
    '2026-06-26 10:30:00',
    'Đồng ý thu hồi toàn bộ lô hàng theo quy chuẩn an toàn thực phẩm. Kích hoạt thông báo dừng phân phối.',
    NULL,
    NULL,
    NULL,
    '2026-06-25 09:00:00',
    '2026-06-26 10:30:00'
),
(
    '00000000-0000-0000-0000-000e00000002',
    @lot_xoai_he_id,
    @user_recorder_id,
    '2026-08-20 14:00:00',
    'Sai quy cách đóng gói và dán nhãn đợt 2, cần thu hồi tái xử lý bao bì',
    'Biên bản kiểm tra kho đóng gói và đối chiếu mẫu nhãn in ngày 20/08/2026',
    'APPROVED',
    @user_manager_id,
    '2026-08-21 09:00:00',
    'Phê duyệt thu hồi cục bộ để thay thế bao bì mới theo chuẩn xuất khẩu.',
    NULL,
    NULL,
    NULL,
    '2026-08-20 14:00:00',
    '2026-08-21 09:00:00'
),
(
    '00000000-0000-0000-0000-000e00000003',
    @lot_buoi_thu_id,
    @user_recorder_id,
    '2026-08-28 15:30:00',
    'Nghi vấn mẫu mã không đồng đều theo phản ánh từ một số thương lái đối tác',
    'Ảnh chụp phản ánh qua Zalo của đại lý phân phối',
    'REJECTED',
    NULL,
    NULL,
    NULL,
    @user_manager_id,
    '2026-08-29 11:00:00',
    'Chưa đủ bằng chứng xác thực. Kết quả kiểm tra mẫu lưu tại kho HTX đạt chuẩn, yêu cầu kiểm tra thêm tại đại lý.',
    '2026-08-28 15:30:00',
    '2026-08-29 11:00:00'
);

-- ------------------------------------------------------------
-- 2. InspectionRequest: 2 yêu cầu (1 PASSED, 1 FAILED)
-- ------------------------------------------------------------
INSERT IGNORE INTO inspection_requests (
    id, production_lot_id, inspection_unit, sample_sent_date, status, created_by, created_at, updated_at, scope_warning, scope_warning_details
) VALUES
(
    '00000000-0000-0000-0000-000f00000001',
    @lot_buoi_thu_id,
    'Trung tâm Phân tích và Thử nghiệm 1',
    '2026-07-25',
    'PASSED',
    @user_manager_id,
    '2026-07-25 08:30:00',
    '2026-07-28 15:00:00',
    FALSE,
    NULL
),
(
    '00000000-0000-0000-0000-000f00000002',
    @lot_cam_dong_id,
    'Trung tâm Kỹ thuật Tiêu chuẩn Đo lường Chất lượng 3',
    '2026-06-20',
    'FAILED',
    @user_manager_id,
    '2026-06-20 09:00:00',
    '2026-06-24 16:30:00',
    FALSE,
    NULL
);

-- Inspection Criteria
INSERT IGNORE INTO inspection_criteria (
    id, inspection_request_id, standard_id, criterion_code, criterion_name
) VALUES
(
    '00000000-0000-0000-0000-001000000001',
    '00000000-0000-0000-0000-000f00000001',
    NULL,
    'VIETGAP_RESIDUE_BUOI',
    'Dư lượng hóa chất BVTV trên bưởi da xanh (Đạt tiêu chuẩn xuất khẩu)'
),
(
    '00000000-0000-0000-0000-001000000002',
    '00000000-0000-0000-0000-000f00000002',
    NULL,
    'PESTICIDE_LIMIT_CAM',
    'Giới hạn dư lượng thuốc BVTV Emamectin benzoate trên cam sành'
);

-- Inspection Criterion Results
INSERT IGNORE INTO inspection_criterion_results (
    id, inspection_criterion_id, result_date, expiry_date, passed, file_path, created_by, created_at
) VALUES
(
    '00000000-0000-0000-0000-001100000001',
    '00000000-0000-0000-0000-001000000001',
    '2026-07-28',
    '2027-07-28',
    TRUE,
    '/uploads/inspection-results/pkn-buoi-2026.pdf',
    @user_manager_id,
    '2026-07-28 15:00:00'
),
(
    '00000000-0000-0000-0000-001100000002',
    '00000000-0000-0000-0000-001000000002',
    '2026-06-24',
    NULL,
    FALSE,
    '/uploads/inspection-results/pkn-cam-2026.pdf',
    @user_manager_id,
    '2026-06-24 16:30:00'
);
