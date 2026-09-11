API: Kết thúc vụ việc thu hồi (RecallCase)

NCL-08-CN-012 — Epic NCL-08: Cảnh báo, thu hồi lô và lịch sử hoạt động

Nhánh git: feature/NCL-08-CN-012-close-recall-case

1. Thông tin chung

Mục tiêu

Cho phép Quản lý hợp tác xã (VT-02) kết thúc vụ việc thu hồi trực tiếp từ danh sách "Yêu cầu thu hồi theo phạm vi ảnh hưởng" đối với các yêu cầu đã duyệt (`APPROVED`), hoặc từ màn hình chi tiết yêu cầu. Sau khi đã xử lý tất cả các lô liên quan và ghi nhận biện pháp khắc phục phòng ngừa (QTN-27):
- Yêu cầu thu hồi hàng loạt chuyển trạng thái sang **"Đã xử lý" (`COMPLETED`)**.
- Các lô hàng trong phạm vi chuyển sang trạng thái `RECALLED`.
- Toàn bộ mã tem của các lô hàng chuyển sang `TraceCodeStatus.RECALLED` và hoàn trả hạn ngạch dải mã (`CodeRange`).
- Vụ việc thu hồi (`RecallCase`) chuyển sang trạng thái `CLOSED`.
- Cập nhật nội dung cảnh báo công khai khi quét mã tem thành: "LÔ HÀNG ĐÃ XỬ LÝ XONG. Vụ việc thu hồi đã đóng ngày dd/mm/yyyy." (QTN-09, QTN-27).
- Hệ thống gửi thông báo cho các doanh nghiệp thu mua liên quan và ghi nhận ActivityLog.

2. Endpoint

PUT /api/v1/recall-requests/bulk/{id}/close

Endpoint chính thức để kết thúc vụ việc thu hồi gắn liền với yêu cầu thu hồi hàng loạt theo phạm vi ảnh hưởng (`id` là UUID của `BulkRecallRequest`).
- Yêu cầu: Quyền `recall:UPDATE` hoặc vai trò `VT-02`.
- Trạng thái yêu cầu trước khi đóng: Phải là `APPROVED`.
- Trạng thái yêu cầu sau khi đóng: Chuyển sang `COMPLETED` ("Đã xử lý").

PUT /api/v1/recall-cases/{id}/close

Endpoint phụ/dịch vụ nội bộ để đóng vụ việc thu hồi trực tiếp theo UUID của `RecallCase`.

GET /api/v1/recall-cases/{id} và GET /api/v1/recall-cases

Endpoint tra cứu dữ liệu vụ việc thu hồi nội bộ (được bảo vệ bởi `@PreAuthorize("hasRole('VT-02')")`).

3. Điều kiện

Người dùng:

- Phải đăng nhập và có vai trò VT-02 (Quản lý hợp tác xã).
- Vụ việc phải thuộc tổ chức của người dùng (cách ly dữ liệu theo `organizationId` trong service — không có quyền xử lý liên tổ chức ở phiên bản này).

Điều kiện về vụ việc:

- Case phải ở trạng thái `OPEN`.
- `req.lotResults` phải phủ hết mọi shipment `RECALLING` thuộc vụ việc; nếu thiếu → lỗi 400 liệt kê các lô còn thiếu.
- `remediationMeasures` bắt buộc (không được rỗng).
- `recoveredQuantity` của từng lô phải ≥ 0 và ≤ `shipment.totalQuantity`.

4. Business Rules

4.1 Lazy materialize (TC-01 / AC)

Khi gọi `GET` list, hệ thống kiểm tra các `ProductionLot` có ≥1 `Shipment` với `status = RECALLING` thuộc tổ chức người dùng mà chưa có bản ghi `RecallCase`. Nếu có, tạo bản ghi `RecallCase` mới (`status = OPEN`) trước khi trả kết quả. Việc tạo này là idempotent (kiểm tra `existsByProductionLotId`; mỗi lô sản xuất chỉ có tối đa một vụ việc).

4.2 Đóng vụ việc (TC-01, TC-02)

Trong cùng một transaction (`RecallCaseServiceImpl.close`):

- Kiểm tra vai trò `VT-02` (QTN-01).
- Kiểm tra `RecallCase` tồn tại và thuộc tổ chức người dùng (`findByIdAndOrganizationId`); không tìm thấy → 404.
- Kiểm tra `status = OPEN`; đã `CLOSED` → 400.
- Kiểm tra `remediationMeasures` không rỗng sau khi trim (QTN-27).
- Phạm vi vụ việc = mọi `Shipment.RECALLING` của lô sản xuất (QTN-24).
- Kiểm tra `req.lotResults` phủ hết các lô đó; thiếu → `BusinessException` liệt kê các lô còn thiếu (TC-02). Chặn lô trùng trong danh sách.
- Kiểm tra từng lô: `resolution` bắt buộc, `recoveredQuantity` trong khoảng `[0, totalQuantity]`; nếu `resolution = UNRECOVERABLE` thì bắt buộc `notes` (lý do + biện pháp xử lý rủi ro).
- Tạo/cập nhật `RecallLotResult` cho từng lô, đánh dấu case `CLOSED`, set `closedBy`/`closedAt`, lưu `remediationMeasures` và `evidenceFileIds`.
- Chuyển trạng thái tất cả các lô hàng trong vụ việc từ `RECALLING` sang `RECALLED`.
- Chuyển trạng thái toàn bộ `TraceCode` của các lô hàng sang `TraceCodeStatus.RECALLED` và hoàn trả hạn ngạch dải mã.
- Gọi `NotificationService.sendRecallCaseClosedNotification(...)` trong cùng transaction (TC-04).
- Ghi lịch sử hoạt động qua `ActivityLogService` (QTN-08).

4.3 Cảnh báo công khai (TC-03, QTN-09, QTN-27)

Sau khi đóng (`CLOSED`), `PublicTraceServiceImpl.resolveRecallMessage()` khi được gọi cho bất kỳ shipment nào thuộc case sẽ trả chuỗi mới:

"LÔ HÀNG ĐÃ XỬ LÝ XONG. Vụ việc thu hồi đã đóng ngày dd/mm/yyyy."

Trong đó `dd/mm/yyyy` là ngày `RecallCase.closedAt` định dạng `dd/MM/yyyy`. Việc tìm vụ việc đóng sử dụng `RecallCaseRepository.findClosedByShipmentId(...)` với `RecallCaseStatus.CLOSED`.

Không xóa bản ghi `TraceCode`, không đặt `status` về bình thường, không ẩn/xóa cảnh báo (QTN-27).

4.4 Thông báo (TC-04)

Hệ thống lấy các tổ chức thu mua từ `ChainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(...)` cho các shipment trong vụ việc, map sang người dùng `ACTIVE` của từng tổ chức, gửi `Notification` qua `NotificationService.sendRecallCaseClosedNotification(caseCode, recipientIds)` đồng bộ trong transaction đóng case.

4.5 Lịch sử kiểm toán (QTN-08)

Mỗi lần đóng case, ghi hoạt động qua `ActivityLogService.logActivity` với `action = CLOSE_RECALL_CASE`, `entityType = RECALL_CASE`, `entityId` = id case, kèm mô tả chứa mã vụ việc, tên lô sản xuất và số lô đã xử lý.

5. Request / Response DTO

5.1 Request — CloseRecallCaseRequest

{
  "remediationMeasures": "Biện pháp khắc phục phòng ngừa: kiểm soát nguồn nguyên liệu, tăng tần suất kiểm nghiệm...",
  "evidenceFileIds": ["UUID", "UUID"],
  "lotResults": [
    {
      "shipmentId": "UUID",
      "resolution": "DESTROYED | RETURNED | REPROCESSED | UNRECOVERABLE",
      "recoveredQuantity": 120,
      "notes": "Ghi chú xử lý (bắt buộc khi UNRECOVERABLE)"
    }
  ]
}

5.2 Response — RecallCaseResponse

{
  "id": "UUID",
  "caseCode": "RC-20260910-153530-A1B2",
  "status": "OPEN | CLOSED",
  "productionLotId": "UUID",
  "productionLotName": "Tên lô sản xuất",
  "organizationId": "UUID",
  "createdAt": "2026-09-10T08:30:00",
  "updatedAt": "2026-09-10T15:00:00",
  "closedAt": "2026-09-10T15:00:00",
  "closedBy": "UUID (user)",
  "remediationMeasures": "...",
  "evidenceFileIds": ["UUID"],
  "lotResults": [
    {
      "id": "UUID (null nếu chưa nhập kết quả)",
      "shipmentId": "UUID",
      "shipmentName": "Tên lô hàng",
      "unit": "Đơn vị từ productionLot.expectedQuantityUnit",
      "resolution": "DESTROYED (null nếu chưa nhập)",
      "recoveredQuantity": 120,
      "notes": "...",
      "createdAt": "..."
    }
  ],
  "shipmentCount": 3
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
      "caseCode": "RC-...",
      "status": "OPEN",
      "createdAt": "...",
      "updatedAt": "...",
      "shipmentCount": 3,
      "closedAt": null,
      "lotResults": []
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
    "caseCode": "RC-...",
    "status": "CLOSED",
    "closedAt": "2026-09-10T15:00:00",
    "closedBy": "...",
    "remediationMeasures": "...",
    "shipmentCount": 3,
    "lotResults": []
  },
  "timestamp": "..."
}

7. Error Response

400 Bad Request — thiếu kết quả cho một hoặc nhiều lô (TC-02)

{
  "success": false,
  "status": 400,
  "message": "Còn 1 lô chưa có kết quả xử lý: <tên lô>. Vui lòng nhập đủ kết quả xử lý cho tất cả các lô."
}

400 Bad Request — `remediationMeasures` rỗng, lô trùng, `resolution`/`recoveredQuantity` thiếu hoặc ngoài phạm vi, `UNRECOVERABLE` thiếu lý do

403 Forbidden — vai trò không phải VT-02

404 Not Found — case không tồn tại hoặc không thuộc tổ chức người dùng

400 Bad Request — case đã `CLOSED` không thể đóng lại

8. Backend xử lý

PUT /api/v1/recall-cases/{id}/close

|
▼
Kiểm tra vai trò VT-02 (QTN-01)
→ 403 nếu sai
|
▼
Kiểm tra `RecallCase` tồn tại và thuộc tổ chức người dùng
→ 404 nếu không có
|
▼
Kiểm tra `status = OPEN`
→ 400 nếu đã `CLOSED`
|
▼
Kiểm tra `remediationMeasures` không rỗng (QTN-27)
→ 400 nếu rỗng
|
▼
Lấy phạm vi: mọi `Shipment.RECALLED` của lô sản xuất (QTN-24)
|
▼
Kiểm tra `lotResults` phủ hết phạm vi, không trùng lô
→ 400 liệt kê lô thiếu (TC-02)
|
▼
Kiểm tra từng lô (`resolution`, `recoveredQuantity`, `UNRECOVERABLE` bắt buộc `notes`)
|
▼
Trong transaction: lưu `RecallLotResult`, đóng case `CLOSED` + `closedBy`/`closedAt`, gửi notification (TC-04), ghi activity log (QTN-08)
|
▼
Trả Response 200 (TC-01, TC-04)

9. Repository

RecallCaseRepository

public interface RecallCaseRepository extends JpaRepository<RecallCase, UUID> {

  List<RecallCase> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

  Optional<RecallCase> findByIdAndOrganizationId(UUID id, UUID organizationId);

  boolean existsByProductionLotId(UUID productionLotId);

  @Query("""
      SELECT DISTINCT rc
      FROM RecallCase rc
      JOIN RecallLotResult lr ON lr.recallCase = rc
      WHERE lr.shipment.id = :shipmentId
        AND rc.status = :status
      ORDER BY rc.closedAt DESC
      """)
  List<RecallCase> findClosedByShipmentId(@Param("shipmentId") UUID shipmentId,
                                          @Param("status") RecallCaseStatus status);

}

RecallLotResultRepository

public interface RecallLotResultRepository extends JpaRepository<RecallLotResult, UUID> {

  List<RecallLotResult> findByRecallCaseId(UUID recallCaseId);

  boolean existsByRecallCaseIdAndShipmentId(UUID recallCaseId, UUID shipmentId);

}

(Sử dụng lại `ChainEventRepository.findDistinctProcurementOrganizationIdsByShipmentIds(...)` cho notification; sửa lại `PublicTraceServiceImpl` cho cảnh báo công khai.)