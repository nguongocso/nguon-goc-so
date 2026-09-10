API: Kết thúc vụ việc thu hồi (RecallCase)

NCL-08-CN-012 — Epic NCL-08: Cảnh báo, thu hồi lô và lịch sử hoạt động

Nhánh git: feature/NCL-08-CN-012-close-recall-case

1. Thông tin chung

Mục tiêu

Cho phép Quản lý hợp tác xã (VT-02) đóng một vụ việc thu hồi sau khi đã xử lý tất cả các lô liên quan, ghi nhận biện pháp khắc phục, và cập nhật nội dung cảnh báo công khai cho các lô đã thu hồi thuộc vụ việc này. Hệ thống gửi thông báo cho các tổ chức thu mua liên quan và giữ nguyên khóa vĩnh viễn trên mã truy xuất.

Nhật ký này phục vụ:

• Đóng vụ việc thu hồi một cách có kiểm soát (chỉ khi mọi lô đã có kết quả xử lý).
• Ghi nhận biện pháp khắc phục phòng ngừa cho truy vết và kiểm toán.
• Đổi nội dung cảnh báo công khai (không ẩn, không xóa) theo quy tắc QTN-09.
• Thông báo cho doanh nghiệp thu mua liên quan qua `NotificationService`.

2. Endpoint

GET /api/v1/recall-cases

Danh sách vụ việc thu hồi (lazy materialize từ các `ProductionLot` có shipment `RECALLED` thuộc tổ chức người dùng).

GET /api/v1/recall-cases/{id}

Chi tiết vụ việc thu hồi kèm danh sách kết quả xử lý từng lô.

PUT /api/v1/recall-cases/{id}/close

Đóng vụ việc thu hồi.

3. Điều kiện

Người dùng:

• Phải đăng nhập và có vai trò VT-02 và thuộc tổ chức sở hữu các `ProductionLot` liên quan, hoặc VT-01 có quyền xử lý liên tổ chức.
• Phải có quyền `RECALL_CASE:CLOSE`.

Điều kiện về vụ việc:

• Case phải ở trạng thái `OPEN`.
• `req.lots` phải phủ hết mọi shipment `RECALLED` thuộc vụ việc; nếu thiếu → lỗi 400.
• `remediationMeasures` bắt buộc (không được rỗng).
• `recoveredQuantity` phải ≥ 0 và ≤ số lượng thu hồi ban đầu (nếu có ràng buộc từ shipment).

4. Business Rules

4.1 Lazy materialize (TC-01 / AC)

Khi gọi `GET` list/detail, hệ thống kiểm tra các `ProductionLot` có ≥1 `Shipment` với `status = RECALLED` thuộc tổ chức người dùng mà chưa có bản ghi `RecallCase`. Nếu có, tạo bản ghi `RecallCase` mới (`status = OPEN`, `createdAt = now`) trước khi trả kết quả. Việc tạo này là idempotent (nếu đã tồn tại thì dùng bản ghi cũ).

4.2 Đóng vụ việc (TC-01, TC-02)

Trong cùng một transaction:

• Kiểm tra `RecallCase` tồn tại và `status = OPEN`.
• Kiểm tra `req.lots` phủ hết mọi `Shipment.RECALLED` của case; nếu thiếu → `BusinessException` liệt kê các lô còn thiếu.
• Kiểm tra `remediationMeasures` không rỗng.
• Tạo hoặc cập nhật `RecallLotResult` cho từng lô với `resolution`, `recoveredQuantity`, `note`.
• Cập nhật `RecallCase.status = CLOSED`, `closedBy`, `closedAt`.
• Gọi `NotificationService.sendRecallCaseClosedNotification(...)` trong cùng transaction (theo pattern `approve`).

4.3 Cảnh báo công khai (TC-03, QTN-09, QTN-27)

Sau khi đóng (`CLOSED`), phương thức `PublicTraceServiceImpl.resolveRecallMessage()` khi được gọi cho bất kỳ shipment nào thuộc case sẽ trả chuỗi mới:

"LÔ HÀNG ĐÃ ĐƯỢC XỬ LÝ. Vụ việc thu hồi đã đóng ngày dd/mm/yyyy."

Trong đó `dd/mm/yyyy` là ngày `RecallCase.closedAt` định dạng theo locale `vi-VN`.

Không xóa bản ghi `TraceCode`, không đặt `status` về bình thường, không thêm/xóa cột `public_warning_message` trên `trace_codes` (QTN-27).

4.4 Thông báo (TC-04)

Hệ thống lấy danh sách `recorded_organization_id` từ các `ChainEvent` liên quan đến các `Shipment` trong vụ việc qua `findDistinctProcurementOrganizationIdsByShipmentIds(...)`, deduplicate, map sang người dùng `ACTIVE` của từng tổ chức, và gửi `Notification` (type = ALERT) qua `NotificationService` đồng bộ trong transaction đóng case.

4.5 Lịch sử kiểm toán

Mỗi lần đóng case, hệ thống ghi `AuditLog` với `action = CLOSE_RECALL_CASE`, `resource_type = "RecallCase"`, `resource_id` là id case, và `new_values` chứa `{status: CLOSED, closedBy, remediationMeasures}`.

5. Request / Response DTO

5.1 Request — CloseRecallCaseRequest

{
  "lots": [
    {
      "shipmentId": "UUID",
      "resolution": "DESTROYED | RETURNED | REPROCESSED | UNRECOVERABLE",
      "recoveredQuantity": 120,
      "note": "Ghi chú xử lý"
    }
  ],
  "remediationMeasures": "Biện pháp khắc phục phòng ngừa: kiểm soát nguồn nguyên liệu, tăng tần suất kiểm nghiệm...",
  "evidenceFileIds": ["UUID", "UUID"]
}

5.2 Response — RecallCaseResponse

{
  "id": "UUID",
  "status": "OPEN | CLOSED",
  "createdAt": "2026-09-10T08:30:00Z",
  "closedAt": "2026-09-10T15:00:00Z",
  "closedBy": "UUID (user)",
  "lotResults": [
    {
      "shipmentId": "UUID",
      "resolution": "DESTROYED",
      "recoveredQuantity": 120,
      "note": "...",
      "createdAt": "..."
    }
  ],
  "remediationMeasures": "...",
  "evidenceFileIds": ["UUID"]
}

5.3 Enum — RecallCaseStatus

OPEN, CLOSED (một chiều; không có PENDING/APPROVED trong phạm vi CN-012).

5.4 Enum — LotResolution

DESTROYED, RETURNED, REPROCESSED, UNRECOVERABLE (tiếng Việt tương ứng trong FE label).

6. Response

GET /api/v1/recall-cases

HTTP 200 OK — danh sách (lazy materialize)

{
  "success": true,
  "status": 200,
  "data": [
    {
      "id": "...",
      "status": "OPEN",
      "createdAt": "...",
      "shipmentCount": 3,
      "closedAt": null
    }
  ],
  "timestamp": "..."
}

PUT /api/v1/recall-cases/{id}/close

HTTP 200 OK — đóng thành công (TC-01)

{
  "success": true,
  "status": 200,
  "data": {
    "id": "...",
    "status": "CLOSED",
    "closedAt": "2026-09-10T15:00:00Z",
    "closedBy": "...",
    "remediationMeasures": "..."
  },
  "timestamp": "..."
}

7. Error Response

400 Bad Request — thiếu kết quả cho một hoặc nhiều lô (TC-02)

{
  "success": false,
  "status": 400,
  "message": "Còn 1 lô chưa có kết quả xử lý: [mã lô]"
}

400 Bad Request — `remediationMeasures` rỗng hoặc `resolution` không hợp lệ

403 Forbidden — không có quyền đóng (`RECALL_CASE:CLOSE`) hoặc khác tổ chức

404 Not Found — case không tồn tại

400 Bad Request — case đã `CLOSED` không thể đóng lại

8. Backend xử lý

PUT /api/v1/recall-cases/{id}/close

│
▼
Kiểm tra quyền VT-02 + `RECALL_CASE:CLOSE` + org boundary (QTN-01)
→ 403 nếu sai
│
▼
Kiểm tra `RecallCase` tồn tại, `status = OPEN`
→ 404 nếu không có; 400 nếu đã `CLOSED`
│
▼
Kiểm tra `req.lots` phủ hết `Shipment.RECALLED` của case
→ 400 liệt kê lô thiếu (TC-02)
│
▼
Kiểm tra `remediationMeasures` không rỗng
→ 400 nếu rỗng
│
▼
Trong transaction: tạo `RecallLotResult`, cập nhật case `CLOSED`, gửi notification, ghi audit
│
▼
Trả Response 200 (TC-01, TC-04)

9. Repository

RecallCaseRepository

public interface RecallCaseRepository extends JpaRepository<RecallCase, UUID> {

  @Query("SELECT rc FROM RecallCase rc WHERE rc.id = :id")
  Optional<RecallCase> findByIdWithDetails(@Param("id") UUID id);

  @Query("SELECT rc FROM RecallCase rc WHERE rc.status = 'OPEN'")
  List<RecallCase> findOpenCases();

}

RecallLotResultRepository

public interface RecallLotResultRepository extends JpaRepository<RecallLotResult, UUID> {

  List<RecallLotResult> findByRecallCaseId(UUID caseId);

}

(Sử dụng lại `ChainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(...)` cho notification; sử dụng lại `PublicTraceServiceImpl` cho cảnh báo công khai.)
