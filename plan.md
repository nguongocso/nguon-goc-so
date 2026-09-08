# Plan: NCL-05-CN-008 + NCL-05-CN-009 — Phiếu Bàn Giao Lô Hàng

> **Branch:** `fix/chuan-hoa-bang-quan-ly-dai-ma-2`
> **Base commit:** `2dea6a19`
> **Ngày bắt đầu:** 2026-09-08

## Danh sách 9 task (đối chiếu Jira)

- [x] NCL-05-CN-008-CV-01: Chốt luồng bàn giao và ranh giới trách nhiệm
- [x] NCL-05-CN-008-CV-02: Thiết kế dữ liệu phiếu bàn giao
- [x] NCL-05-CN-008-CV-03: Thiết kế màn hình tạo phiếu bàn giao
- [x] NCL-05-CN-008-CV-04: Phát triển tạo và hủy phiếu bàn giao
- [x] NCL-05-CN-008-CV-05: Kiểm thử tạo phiếu bàn giao
- [ ] NCL-05-CN-009-CV-01: Chốt quy tắc xác nhận, từ chối và hết hiệu lực
- [ ] NCL-05-CN-009-CV-02: Thiết kế màn hình xác nhận nhận bàn giao
- [ ] NCL-05-CN-009-CV-03: Phát triển xác nhận và chuyển quyền ghi sự kiện
- [ ] NCL-05-CN-009-CV-04: Kiểm thử luồng xác nhận bàn giao

## Acceptance Criteria

### NCL-05-CN-008 (Tạo phiếu):
- TC-01: lô còn 1 tấn, tạo phiếu 8 tạ → PENDING_CONFIRMATION, notification
- TC-02: lô còn 5 tạ, tạo 8 tạ → chặn, báo "vượt lượng còn lại"
- TC-03: lô đang thu hồi → chặn, báo "lô đang bị thu hồi"
- TC-04: hủy phiếu chưa xác nhận kèm lý do → CANCELLED, nhãn "đang bàn giao" biến mất

### NCL-05-CN-009 (Xác nhận/Từ chối):
- TC-01: xác nhận → ACCEPTED, ChainEvent HANDOVER, trách nhiệm chuyển
- TC-02: từ chối kèm lý do → REJECTED, notification kèm lý do
- TC-03: quá hạn → EXPIRED, cả 2 bên nhận notification, idempotent
- TC-04: tổ chức ngoài cuộc mở phiếu → 403
