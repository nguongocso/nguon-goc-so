# API Docs - Phạm vi công nhận của đơn vị kiểm nghiệm (NCL-11-CN-006 Phase 2)

Cập nhật theo code hiện tại: 2026-08-28

## 1. Mô tả

Mỗi đơn vị kiểm nghiệm trong danh mục dùng chung (NCL-11-CN-006 Phase 1) có thể
khai báo **phạm vi công nhận**: tập chỉ tiêu kiểm nghiệm (từ danh mục dùng chung
`inspection_criterion_catalog` của NCL-09-CN-009) mà đơn vị được công nhận thực hiện.

Khi tạo yêu cầu kiểm nghiệm chọn đơn vị từ danh mục, hệ thống:
- So sánh bộ chỉ tiêu gửi đi với phạm vi công nhận của đơn vị.
- Nếu có chỉ tiêu **ngoài phạm vi**, lưu cảnh báo trên yêu cầu
  (`scope_warning = true` + danh sách tên chỉ tiêu) và trả về trong response.
- **Không chặn** tạo yêu cầu — cảnh báo chỉ mang tính thông tin.

## 2. Quản lý phạm vi công nhận (VT-01)

### `GET /api/v1/testing-units/{testingUnitId}/accreditation-scopes`

- Quyền: mọi vai trò đã xác thực (dùng để hiển thị cảnh báo khi tạo yêu cầu).
- Trả về tóm tắt phạm vi công nhận hiện tại của đơn vị.

Response `data`:

```json
{
  "testingUnitId": "8ab3b62f-...",
  "testingUnitName": "Lab ABC",
  "accreditedCriteria": [
    { "id": 1, "code": "CT1", "name": "CT1" },
    { "id": 2, "code": "CT2", "name": "CT2" }
  ]
}
```

### `PUT /api/v1/testing-units/{testingUnitId}/accreditation-scopes`

- Quyền: `VT-01`.
- Ngữ nghĩa **REPLACE-ALL**: danh sách gửi lên là toàn bộ chỉ tiêu thuộc phạm vi
  sau khi lưu (không phải delta).
- Chỉ tiêu phải tồn tại và `ACTIVE` trong danh mục dùng chung; nếu không → `400`.

Request body:

```json
{
  "criterionDefinitionIds": [1, 2, 3]
}
```

Response `data`: giống `GET` (trả về phạm vi vừa lưu).

## 3. Tích hợp vào tạo yêu cầu kiểm nghiệm

`POST /api/v1/production-lots/{lotId}/test-requests` (xem [inspection-request.md](inspection-request.md))

Khi `testingUnitId` được gửi kèm, response bổ sung 2 trường:

```json
{
  "hasScopeWarning": true,
  "scopeWarningDetails": "CT3"
}
```

- `hasScopeWarning`: `true` khi đơn vị **có** phạm vi công nhận được cấu hình
  và có ít nhất một chỉ tiêu được chọn nằm ngoài phạm vi.
- `scopeWarningDetails`: tên các chỉ tiêu ngoài phạm vi (ngăn cách dấu phẩy).
- Nếu đơn vị chưa được cấu hình phạm vi (danh sách rỗng), không phát sinh cảnh báo
  để tránh nhiễu cho dữ liệu Phase 1.
- Việc lưu cảnh báo được thực hiện trong cùng giao dịch tạo yêu cầu; không làm
  thay đổi bất kỳ luồng nghiệp vụ/chặn nào hiện có.

## 4. Cấu trúc dữ liệu

- Bảng `accreditation_scopes`: `id` (UUID), `testing_unit_id`, `criterion_id`,
  `criterion_code`, `criterion_name` (snapshot), `created_at`.
  Unique `(testing_unit_id, criterion_id)`, FK cascade tới `testing_units` và
  `inspection_criterion_catalog`.
- Bảng `inspection_requests` bổ sung cột `scope_warning` (BOOLEAN, default false)
  và `scope_warning_details` (VARCHAR(2000), nullable).

## 5. File tham chiếu

- `backend/src/main/java/vn/nguongocso/certification/entity/AccreditationScope.java`
- `backend/src/main/java/vn/nguongocso/certification/service/AccreditationScopeService.java`
- `backend/src/main/java/vn/nguongocso/certification/controller/TestingUnitController.java`
- `backend/src/test/java/vn/nguongocso/certification/service/AccreditationScopeServiceImplTest.java`
- `frontend/src/pages/admin/TestingUnitScopeManagerPage.tsx`
- `frontend/src/pages/certification/CreateInspectionRequestPage.tsx`
