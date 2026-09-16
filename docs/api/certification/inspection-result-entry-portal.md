# NCL-11-CN-007 – Cổng nhập kết quả dành cho đơn vị kiểm nghiệm

> **Epic:** `NCL-11` – Sơ chế và kiểm nghiệm chất lượng
> **Tài liệu:** API Contract / Contract-first
> **Trạng thái:** Approved for implementation
> **Phạm vi:** `NCL-11-CN-007-CV-01` đến `NCL-11-CN-007-CV-05`

## 1. Mục tiêu và nguồn yêu cầu

Cho phép đơn vị kiểm nghiệm nhập kết quả cho đúng một yêu cầu qua liên kết có thời hạn, dùng một lần và không cần tài khoản. Hệ thống phân biệt kết quả do đơn vị kiểm nghiệm khai với kết quả do HTX nhập, nhưng giữ nguyên toàn bộ logic đạt/không đạt và hiệu lực hiện hành.

| Nguồn | Hành vi |
|---|---|
| `TC-01` | Submit hợp lệ, lưu nguồn đơn vị kiểm nghiệm |
| `TC-02` | Link hết hạn bị từ chối và hướng dẫn cấp lại |
| `TC-03` | Link đã dùng không được replay |
| `TC-04` | Luồng HTX nhập tay lưu nguồn HTX |
| `QTN-14` | Token có thời hạn, dùng một lần |
| `QTN-20` | Token hẹp theo tenant/request, hash-only và có hạn mức |
| `QTN-21` | Dùng chung logic trạng thái và hiệu lực |

## 2. Quyết định nghiệp vụ

- Chỉ `VT-02` được cấp/cấp lại link cho request `PENDING_RESULT` thuộc tổ chức hiện tại và có `testingUnitId`.
- Link mặc định 7 ngày; cho phép chọn 1–30 ngày. Link mới thu hồi link `ACTIVE` cũ.
- Public submit phải bao phủ toàn bộ chỉ tiêu và dùng chung validation/status/expiry với luồng nội bộ.
- Nguồn kết quả là `COOPERATIVE_MANUAL` hoặc `TESTING_UNIT_PORTAL`.
- Mọi mutation thủ công ghi nguồn manual và thu hồi link active.
- `USED`, `REVOKED`, `EXPIRED` là trạng thái cuối; không bổ sung trạng thái vào `InspectionRequestStatus`.

```text
Không có --VT-02 cấp--> ACTIVE
ACTIVE --public submit thành công--> USED
ACTIVE --cấp lại/manual mutation--> REVOKED
ACTIVE --now >= expiresAt--> EXPIRED
```

## 3. API dành cho VT-02

### 3.1 Cấp hoặc cấp lại link

`POST /api/v1/inspection-requests/{requestId}/result-entry-links`

- Access JWT; role `VT-02`; request phải thuộc organization hiện tại.
- Thành công: `201 Created`.

```json
{
  "recipientEmail": "lab@example.vn",
  "expiryDays": 7
}
```

`recipientEmail` bắt buộc, đúng định dạng và tối đa 255 ký tự. `expiryDays` mặc định 7, tối thiểu 1, tối đa 30.

```json
{
  "id": "0f336c71-38bb-4c17-a9aa-6c49373d9131",
  "status": "ACTIVE",
  "recipientEmail": "lab@example.vn",
  "expiresAt": "2026-09-23T10:00:00",
  "usedAt": null,
  "createdAt": "2026-09-16T10:00:00",
  "entryUrl": "https://frontend.example/inspection-result-entry/<raw-token>"
}
```

`entryUrl` chỉ xuất hiện trong response cấp link và email. API không trả `tokenHash` và không ghi raw token/full URL vào log.

### 3.2 Xem link mới nhất

`GET /api/v1/inspection-requests/{requestId}/result-entry-links/latest`

- Auth/tenant giống endpoint cấp link; thành công `200 OK`.
- Response gồm `id`, `status`, `recipientEmail`, `expiresAt`, `usedAt`, `createdAt`.
- Không trả `entryUrl`, raw token hoặc token hash; `404` nếu chưa từng cấp.

## 4. API public

Không yêu cầu JWT. Token quyết định toàn bộ request/tenant scope; client không truyền organization/request ID để chọn phạm vi. Mọi response có `Cache-Control: no-store`, `Pragma: no-cache`, `Referrer-Policy: no-referrer`.

### 4.1 Mở cổng nhập kết quả

`GET /api/v1/public/inspection-result-entry/{token}`

```json
{
  "testingUnit": "Trung tâm kiểm nghiệm A",
  "lotCode": "LOT-2026-00123",
  "lotName": "Lô xoài tháng 9",
  "sampleSentDate": "2026-09-15",
  "expiresAt": "2026-09-23T10:00:00",
  "criteria": [
    {
      "criterionId": "b83685c9-f6c0-42f4-852c-263450a760d2",
      "code": "RESIDUE_PESTICIDE",
      "name": "Dư lượng thuốc bảo vệ thực vật",
      "standardName": "VietGAP"
    }
  ]
}
```

Không trả organization ID, request/lot UUID, testing unit ID, user/creator ID, email người nhận hoặc đường dẫn file nội bộ.

### 4.2 Upload phiếu

`POST /api/v1/public/inspection-result-entry/{token}/criteria/{criterionId}/file`

- `multipart/form-data`, field `file`.
- JPG/PNG/PDF, tối đa 5 MB; tên file do server sinh.
- Token phải active/chưa hết hạn; criterion phải thuộc đúng request.
- Response trả opaque `filePath` chỉ dùng được khi submit cùng token và criterion.

### 4.3 Submit toàn bộ kết quả

`PUT /api/v1/public/inspection-result-entry/{token}/results`

```json
{
  "results": [
    {
      "criterionId": "b83685c9-f6c0-42f4-852c-263450a760d2",
      "resultDate": "2026-09-16",
      "expiryDate": "2027-09-16",
      "passed": true,
      "filePath": "inspection-results/opaque-handle.pdf"
    }
  ]
}
```

Transaction phải: validate token/request/tenant/testing unit; validate đủ toàn bộ criterion, ngày và file; consume atomically `ACTIVE → USED`; lưu `entrySource=TESTING_UNIT_PORTAL`, `createdBy=null`, `portalLinkId`; chốt status và quét cảnh báo bằng logic hiện hành; lưu usedAt/IP/user-agent. Nếu bất kỳ bước nào lỗi thì rollback và token vẫn active.

Response `200 OK` là `InspectionCriterionResultResponse[]`, bổ sung additive `entrySource`; `createdByName` có thể null.

## 5. Error contract

| HTTP | Trường hợp |
|---:|---|
| `400` | Email, expiry, payload, ngày hoặc file không hợp lệ |
| `403` | Actor authenticated không phải `VT-02` |
| `404` | Request không tồn tại/khác tenant; token không hợp lệ dùng thông điệp generic |
| `409` | Request không còn `PENDING_RESULT` hoặc xung đột manual/public |
| `410` | Link hết hạn, đã dùng hoặc đã bị thay thế |
| `413` | File vượt 5 MB |
| `415` | MIME không thuộc JPG/PNG/PDF |
| `429` | Vượt hạn mức portal |

Thông điệp hết hạn hướng dẫn liên hệ HTX để cấp lại. Token không hợp lệ không làm lộ tenant hoặc tài nguyên nội bộ.

## 6. Bảo mật và tenant isolation

- Token 32 byte từ `SecureRandom`, URL-safe; DB chỉ lưu SHA-256 và prefix audit.
- Authenticated query scope bằng `requestId + currentOrganizationId`.
- Link lưu snapshot organization/testing unit và service kiểm chéo với request.
- Criterion/file handle phải thuộc đúng request của token.
- Conditional update bảo vệ replay/double-submit.
- Hạn mức: 60 request/giờ/valid token, 30 invalid-token request/giờ/IP. Rate limit in-memory không đồng bộ đa instance; đây là giới hạn vận hành đã biết.
- Không log raw token, full URL hoặc request body chứa secret.

## 7. Database/migration

Tạo `inspection_result_entry_links` với request, organization, testing unit, email snapshot, token prefix/hash, status, expiry/use/revoke và actor cấp link.

Mở rộng `inspection_criterion_results`:

- `entry_source VARCHAR(32) NOT NULL DEFAULT 'COOPERATIVE_MANUAL'`;
- `portal_link_id CHAR(36) NULL`;
- `created_by` nullable cho public actor.

Dữ liệu cũ backfill `COOPERATIVE_MANUAL`; migration additive, không sửa migration đã áp dụng.

## 8. Tác động frontend

- VT-02 có dialog cấp/cấp lại link, nhập email/thời hạn và xem metadata link mới nhất.
- Route `/inspection-result-entry/:token` nằm ngoài `PrivateRoute`.
- Public page có loading, active, invalid, expired, used/revoked, submitting và success terminal.
- Axios không gửi access token hoặc kích hoạt logout redirect cho portal public.
- Map source: `TESTING_UNIT_PORTAL` → “Đơn vị kiểm nghiệm khai”; `COOPERATIVE_MANUAL` → “Hợp tác xã nhập”.

## 9. Kiểm thử bắt buộc

- [ ] Role/tenant issue-reissue; raw token chỉ trả một lần và DB chỉ chứa hash.
- [ ] Public GET/upload/submit happy path ghi source portal và consume link.
- [ ] Expired/used/revoked/invalid token không trả dữ liệu request.
- [ ] Hai submit đồng thời chỉ một lần thành công.
- [ ] Criterion/file khác request bị từ chối.
- [ ] Payload thiếu/trùng criterion rollback toàn bộ và giữ token active.
- [ ] Manual mutation ghi source manual và revoke link active.
- [ ] `createdBy=null` không gây NPE ở API/public trace.
- [ ] QTN-21/cảnh báo hiệu lực giống manual; rate limit và no-store hoạt động.
- [ ] UI states, test/lint/build và browser runtime pass.

## 10. Không hồi quy và contract cuối

- Giữ nguyên manual API, request statuses, QTN-21, expiry warning, seal gate và reinspection.
- Public trace chỉ bổ sung `entrySource`, không rename/remove field.
- Activity log manual vẫn có user/organization; public submit không tạo anonymous activity log.
- Không thay đổi testing unit CRUD, partner API key, invitation hoặc password reset.

Contract khóa expiry 7 ngày (1–30), email xác nhận lúc cấp, per-result provenance, public actor không có user, upload tùy chọn và rate limit in-memory giai đoạn đầu. Thay đổi API/DB/quyền/tenant/state ngoài phạm vi phải quay lại Planning Gate.
