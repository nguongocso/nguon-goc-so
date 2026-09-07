# NCL-11-CN-005 — API Design: Xử lý lô có kết quả kiểm nghiệm không đạt (QTN-30)

> Loại tài liệu: Phân tích nghiệp vụ + Thiết kế API (KHÔNG kèm implement).
> Người dùng: Backend Agent, Frontend Agent.
> Nhãn trạng thái: `EXISTING` (đã có) · `REUSE` (dùng lại) · `MODIFY` (cần mở rộng) · `NEW` (cần tạo) · `PROPOSED` (đề xuất tên mới) · `UNKNOWN` (chưa đủ bằng chứng).
> Ngày phân tích: 2026-09-05.
> Nguồn đối chiếu: module `trace`, `certification`, `farm` của backend và các tài liệu hiện hành trong `docs/api/`.

---

## 1. Tổng quan

Story NCL-11-CN-005 số hóa xử lý lô sản xuất có kết quả kiểm nghiệm **Không đạt** theo business rule **QTN-30 — Lô chưa đạt kiểm nghiệm không được tạo lô hàng**:

1. Lô sản xuất thuộc loại nông sản **bắt buộc kiểm nghiệm** (`product_categories.requires_inspection = true`) có kết luận **Không đạt** phải bị chặn **ngay tại API tạo Lô hàng** (`POST /api/v1/shipments`), không chỉ ở bước kích hoạt tem.
2. Không được tạo mã/tem truy xuất cho lô chưa đạt.
3. Kết quả Không đạt không được ẩn khỏi hồ sơ truy xuất.
4. Hệ thống phải yêu cầu xử lý theo một trong hai hướng: **loại bỏ lô** hoặc **kiểm nghiệm lại**.

Nguyên tắc phân tách khái niệm bắt buộc khi thiết kế (không được làm lẫn hai khái niệm):

```text
Production Lot (Lô sản xuất)
      ↓
Inspection (yêu cầu + kết quả kiểm nghiệm)
      ↓
Production Lot đạt (kết quả mới nhất còn hiệu lực)
      ↓
Create Shipment Lot (POST /api/v1/shipments)
```

---

## 2. Kết quả phân tích repository

### 2.1 Bảng đồ API liên quan hiện có

| Nghiệp vụ | API | Triển khai hiện tại | Nhãn |
|---|---|---|---|
| Tạo Lô hàng (+ sinh mã truy xuất) | `POST /api/v1/shipments` | `ShipmentServiceImpl.createShipment`: role VT-02 → lô tồn tại → tổ chức → `status == PACKAGED` → hạn mức dải mã. **Không có gate kiểm nghiệm** | EXISTING → MODIFY |
| Kích hoạt tem | `POST /api/v1/shipments/{id}/activate` | `ShipmentServiceImpl.activateShipmentStamps`: role VT-02 → lô hàng tồn tại → tổ chức → lô `PACKAGED` → lô hàng `CODE_PRINTED`. **Không enforce kết quả kiểm nghiệm** | EXISTING → MODIFY |
| Pre-check điều kiện kích hoạt tem | `POST /api/v1/production-lots/{lotId}/can-activate-seal` | Tính kết quả mới nhất theo mã chỉ tiêu trên toàn bộ yêu cầu của lô; trả `canActivate`, `reason`, thống kê chỉ tiêu | EXISTING → REUSE |
| Tạo yêu cầu kiểm nghiệm | `POST /api/v1/production-lots/{lotId}/test-requests` | Cho phép tạo vòng mới kể cả khi yêu cầu trước `FAILED` (chỉ chặn trùng bộ chỉ tiêu với yêu cầu đang `PENDING_RESULT`, trừ khi `confirmDuplicate = true`) | EXISTING → REUSE (kiểm nghiệm lại) |
| Ghi kết quả kiểm nghiệm | `POST /api/v1/inspection-criteria/{criterionId}/results`, `PUT /api/v1/inspection-requests/{requestId}/results` | Cho phép ghi/cập nhật khi request `PENDING_RESULT` hoặc `FAILED`; tự chốt `PASSED`/`FAILED` | EXISTING → REUSE |
| Lịch sử kiểm nghiệm | `GET /api/v1/test-requests?lotId=…`, `GET /api/v1/inspection-requests/{requestId}`, `GET /api/v1/inspection-requests/{requestId}/results` | Trả cả yêu cầu `FAILED` lẫn `PASSED`; item có `failedCriteriaCount`, `failedRatio` | EXISTING → REUSE |
| Tra cứu công khai | `publicapi` (Public Lookup) | `PublicInspectionResponse` có `totalCriteria`, `passedCriteria`, `failedCriteriaCount`, `failedRatio` — kết quả Không đạt hiển thị công khai | EXISTING → REUSE |
| Xóa kết quả kiểm nghiệm | `DELETE /api/v1/inspection-results/{resultId}` | Xóa kết quả **không ràng buộc trạng thái yêu cầu** → có thể xóa bằng chứng Không đạt của yêu cầu đã kết luận | EXISTING → MODIFY (TC-04) |
| Cập nhật trạng thái lô sản xuất | `POST /api/v1/production-lots/{id}/submit` `/approve` `/cancel`, `PUT /api/v1/production-lots/{id}` | `ProductionLotStatus` hiện có: `DRAFT, PENDING, APPROVED, REJECTED, HARVESTED, PREPROCESSED, PACKAGED, CLOSED, RECALLED, CANCELLED` — **không có trạng thái loại bỏ** | EXISTING → MODIFY |
| Loại bỏ lô sản xuất | — | Không tồn tại; không có cột `disposal_*` trên `production_lot` | NOT_FOUND → NEW |
| Sản lượng dự kiến | `CreateProductionLotRequest.expectedQuantity`, `GET /api/v1/production-lots/{id}`, dashboard | Lưu trên `production_lot` (`expected_quantity`, `expected_quantity_unit`, `actual_quantity`). Lưu ý: `totalQuantity` khi tạo lô hàng chưa được đối chiếu với `actualQuantity` (ngoài phạm vi story này) | EXISTING |
| Lịch sử xử lý (audit) | `GET /api/v1/organizations/activity-logs` | Nhật ký hoạt động theo tổ chức, lọc `action`, khoảng thời gian | EXISTING → REUSE |

### 2.2 Các khoảng trống so với QTN-30

| # | Khoảng trống | Bằng chứng trong code | Hệ quả |
|---|---|---|---|
| GAP-1 | `POST /api/v1/shipments` không kiểm tra kết quả kiểm nghiệm | `ShipmentServiceImpl.createShipment` chỉ validate `PACKAGED` + hạn mức dải mã | Lô Không đạt vẫn tạo được lô hàng và sinh mã/tem — vi phạm trực tiếp QTN-30 |
| GAP-2 | `POST /api/v1/shipments/{id}/activate` không enforce kết quả kiểm nghiệm | `activateShipmentStamps` chỉ validate `PACKAGED` + `CODE_PRINTED` | `can-activate-seal` chỉ là API pre-check, không có rào chắn thực thi |
| GAP-3 | Không có API "loại bỏ lô" | Không có endpoint, không có cột `disposal_reason`/`handling_measure`/`disposed_*` trên `production_lot` | Hướng xử lý 1 của QTN-30 không thực thi được |
| GAP-4 | `DELETE /api/v1/inspection-results/{resultId}` cho phép xóa kết quả của yêu cầu đã kết luận `FAILED` | `InspectionCriterionResultServiceImpl.deleteResult` chỉ kiểm tra truy cập tổ chức | Có thể xóa kết quả Không đạt rồi ghi lại kết quả Đạt — vi phạm TC-04 |
| GAP-5 | `POST /api/v1/production-lots/{id}/cancel` không phân biệt lô chưa đạt | `cancelProductionLot` cho phép hủy mọi lô chưa sinh mã | Lô Không đạt có thể bị "gỡ" khỏi hệ thống mà không ghi biện pháp xử lý |
| GAP-6 | Tài liệu `trace/CreateShipmentTraceCode.md` ghi lỗi sai trạng thái/hạn mức là `409`, code thực tế trả `400` | `BusinessException(String)` mặc định `BAD_REQUEST` | Không nhất quán doc/code (tồn tại từ trước; story này không bắt buộc sửa) |

> **Lưu ý:** "Không đạt kiểm nghiệm" KHÔNG cần thêm giá trị enum vào `ProductionLotStatus`. Đây là **trạng thái suy diễn** từ dữ liệu kiểm nghiệm (xem D-3). Chỉ cần thêm **một** trạng thái mới: `DISPOSED` cho lô đã bị loại bỏ.

---

## 3. Luồng nghiệp vụ đích

```text
Lô sản xuất (loại bắt buộc kiểm nghiệm)
      ↓
POST /api/v1/shipments
      ├── Kết quả mới nhất theo từng chỉ tiêu: tất cả Đạt + còn hiệu lực
      │        → tạo lô hàng + sinh mã (luồng bình thường)
      └── Ngược lại → 409 CONFLICT
               (chưa có kết quả / đang chờ / Không đạt / hết hạn)
               ↓
      ┌──────────────────────────┬─────────────────────────────────┐
      ▼                          ▼
Loại bỏ lô                    Kiểm nghiệm lại
POST /production-lots/{id}/dispose
                              POST /production-lots/{lotId}/test-requests (vòng N)
  → status DISPOSED             → PUT /inspection-requests/{requestId}/results
    (trạng thái cuối)            → yêu cầu mới PASSED
                                   → quay lại luồng bình thường
```

Trạng thái `ProductionLot` mở rộng (PROPOSED):

```text
HARVESTED → PREPROCESSED → PACKAGED → (tạo lô hàng …) → CLOSED
                                   ├── CANCELLED (luồng hủy hiện có — bị chặn khi lô chưa đạt, xem §5.4)
                                   ├── DISPOSED  (mới — loại bỏ sau kết luận Không đạt, trạng thái cuối)
                                   └── RECALLED  (luồng thu hồi hiện có)
```

---

## 4. Tổng hợp contract

| # | Nghiệp vụ | API | Nhãn | Thay đổi so với hiện tại |
|---|---|---|---|---|
| 1 | Chặn tạo Lô hàng từ lô chưa đạt (TC-01) | `POST /api/v1/shipments` | MODIFY | Thêm gate QTN-30 sau kiểm tra `PACKAGED` |
| 2 | Chặn kích hoạt tem từ lô chưa đạt | `POST /api/v1/shipments/{id}/activate` | MODIFY | Thêm gate QTN-30/QTN-21 trước khi chuyển `ACTIVATED` |
| 3 | Loại bỏ lô sản xuất (TC-03) | `POST /api/v1/production-lots/{id}/dispose` | NEW | Endpoint + trạng thái `DISPOSED` + cột `disposal_*` |
| 4 | Chặn hủy lô chưa đạt | `POST /api/v1/production-lots/{id}/cancel` | MODIFY | Thêm điều kiện loại trừ khi kết luận mới nhất là `FAILED` |
| 5 | Chặn tạo yêu cầu kiểm nghiệm cho lô đã loại bỏ | `POST /api/v1/production-lots/{lotId}/test-requests` | MODIFY | Thêm `DISPOSED` vào danh sách trạng thái loại trừ |
| 6 | Không cho xóa kết quả của yêu cầu đã kết luận (TC-04) | `DELETE /api/v1/inspection-results/{resultId}` | MODIFY | Chỉ cho phép xóa khi request còn `PENDING_RESULT` |
| 7 | Kiểm nghiệm lại (TC-02) | `POST /test-requests` → `PUT /{requestId}/results` → `POST /can-activate-seal` | REUSE | Không đổi contract |
| 8 | Lịch sử kiểm nghiệm cả Không đạt lẫn Đạt (TC-04) | `GET /api/v1/test-requests?lotId=…`, `GET /api/v1/inspection-requests/{requestId}` | REUSE | Không đổi contract |
| 9 | Tra cứu công khai hiển thị kết quả Không đạt (TC-04) | `publicapi` lookup | REUSE | Không đổi contract |
| 10 | Lịch sử xử lý (loại bỏ, xóa kết quả…) | `GET /api/v1/organizations/activity-logs` | REUSE | Không đổi contract |

---

## 5. Đặc tả endpoint

### 5.1 [MODIFY] `POST /api/v1/shipments` — gate QTN-30 (TC-01)

Contract hiện có giữ nguyên (xem `trace/CreateShipmentTraceCode.md`). Phần dưới chỉ mô tả thay đổi.

| Thuộc tính | Giá trị |
|---|---|
| **Method / Endpoint** | `POST /api/v1/shipments` |
| **Authentication** | JWT `Authorization: Bearer <token>` |
| **Authorization** | role `VT-02` (kiểm tra trong service — giữ nguyên) |
| **Path / Query parameter** | không đổi |
| **Request body** | không đổi (`productionLotId`, `name`, `totalQuantity`, `packagingInfo`) |
| **Response thành công** | `201 CREATED` — `ShipmentResponse` (không đổi) |

**Validation mới (chèn sau bước "lô phải PACKAGED", trước kiểm tra dải mã):**

Nếu `productionLot.productCategory.requiresInspection == true`:

1. Lấy bộ chỉ tiêu `ACTIVE` được gán cho loại nông sản qua `category_criteria`.
2. Với mỗi chỉ tiêu, lấy **kết quả mới nhất** theo `criterionCode` trên **toàn bộ yêu cầu kiểm nghiệm của lô** (cùng logic `checkCanActivateSeal` — mới hơn theo `resultDate`, phụ `updatedAt`).
3. Lô đủ điều kiện khi **mọi** chỉ tiêu có kết quả mới nhất `passed = true` và `expiryDate >= ngày hiện tại`.
4. Không đủ điều kiện → ném `BusinessException(HttpStatus.CONFLICT, message, details)` — **không tạo Shipment, không sinh TraceCode, không trừ hạn mức dải mã**.

`message` và `errors.reasonCode` theo bảng:

| Tình trạng kết quả của lô | `reasonCode` (PROPOSED) | `message` |
|---|---|---|
| Chưa có kết quả nào / chưa đủ cho mọi chỉ tiêu | `INSPECTION_MISSING` | "Lô sản xuất chưa có kết quả kiểm nghiệm đạt cho tất cả chỉ tiêu, không thể tạo lô hàng." |
| Còn yêu cầu đang `PENDING_RESULT` | `INSPECTION_PENDING` | "Lô sản xuất đang chờ kết quả kiểm nghiệm, không thể tạo lô hàng." |
| Kết luận mới nhất là Không đạt | `INSPECTION_FAILED` | "Lô sản xuất chưa đạt kiểm nghiệm, không thể tạo lô hàng." |
| Kết quả đạt đã hết hạn (`expiryDate < today`) | `INSPECTION_EXPIRED` | "Kết quả kiểm nghiệm đã hết hiệu lực, không thể tạo lô hàng." |

Thứ tự ưu tiên khi trùng nhiều tình trạng: `INSPECTION_FAILED` → `INSPECTION_PENDING` → `INSPECTION_EXPIRED` → `INSPECTION_MISSING`.

**Response lỗi (409 CONFLICT):**

```json
{
  "success": false,
  "status": 409,
  "message": "Lô sản xuất chưa đạt kiểm nghiệm, không thể tạo lô hàng.",
  "errors": {
    "reasonCode": "INSPECTION_FAILED",
    "totalCriteria": 5,
    "passedCriteria": 3,
    "failedOrExpiredCriteria": 2,
    "earliestExpiryDate": null
  },
  "path": "/api/v1/shipments",
  "timestamp": "2026-09-05T03:00:00Z"
}
```

> `errors` tái dùng shape thống kê của `CanActivateSealCheckResponse` (PROPOSED phần `reasonCode`).

**Các lỗi giữ nguyên từ contract hiện có** (đang trả `400` theo code — xem GAP-6):

| HTTP | Trường hợp | Message hiện tại |
|---|---|---|
| 400 | Không đúng role | "Bạn không có quyền tạo lô hàng." |
| 400 | Lô không tồn tại | "Không tìm thấy lô sản xuất." |
| 400 | Lô không thuộc tổ chức | "Bạn không thuộc tổ chức của lô sản xuất." |
| 400 | Lô sai trạng thái | "Chỉ có thể tạo lô hàng từ lô sản xuất đã đóng gói." |
| 400 | Dải mã / hạn mức | "Tổ chức chưa được cấp dải mã truy xuất." / "Số lượng tem vượt quá hạn mức dải mã còn lại." |

### 5.2 [MODIFY] `POST /api/v1/shipments/{id}/activate` — enforce gate tại kích hoạt tem

| Thuộc tính | Giá trị |
|---|---|
| **Method / Endpoint** | `POST /api/v1/shipments/{id}/activate` |
| **Authentication** | JWT `Authorization: Bearer <token>` |
| **Authorization** | role `VT-02`, lô hàng thuộc tổ chức của người dùng (giữ nguyên) |
| **Path parameter** | `id` — UUID lô hàng |

**Thay đổi:** sau các kiểm tra hiện có (`PACKAGED`, `CODE_PRINTED`, chưa `ACTIVATED`), thực hiện **cùng gate QTN-30 như §5.1** trên `shipment.productionLot`. Không thỏa → `409 CONFLICT` với cùng `message` / `errors.reasonCode`.

**Lý do:** QTN-21 yêu cầu chặn kích hoạt tem cho lô chưa đạt; hiện `activateShipmentStamps` không kiểm tra (GAP-2). Gate tại đây là rào chắn thứ hai, không thay thế gate tại API tạo lô hàng.

**Impact:** lô hàng đã tồn tại từ trước (tạo trước khi gate có hiệu lực) từ lô chưa đạt sẽ không thể kích hoạt tem — cần policy xử lý dữ liệu lọt (xem §12).

Các lỗi khác giữ nguyên: `400` "Không tìm thấy lô hàng.", `400` tổ chức khác, `400` "Tem đã được kích hoạt trước đó.", `400` "Lô hàng chưa được cấp hoặc in mã tem."

### 5.3 [NEW] `POST /api/v1/production-lots/{id}/dispose` — loại bỏ lô sản xuất (TC-03)

| Thuộc tính | Giá trị |
|---|---|
| **Method** | `POST` |
| **Endpoint** | `/api/v1/production-lots/{id}/dispose` |
| **Authentication** | JWT `Authorization: Bearer <token>` |
| **Authorization** | `@PreAuthorize("hasRole('VT-02')")` + `permissionChecker.check("PRODUCTION_LOT", "UPDATE")` (theo pattern `/cancel`) |
| **Path parameter** | `id` — UUID lô sản xuất |
| **Query parameter** | không có |
| **Business error code** | project không dùng mã lỗi nghiệp vụ; lỗi phân biệt qua HTTP status + `message` (+ `errors` khi có) |

**Request body:**

```json
{
  "reason": "Kết quả kiểm nghiệm không đạt dư lượng thuốc bảo vệ thực vật",
  "handlingMeasure": "Phá hủy toàn bộ 200 kg tại khu cách ly theo hướng dẫn của cơ quan quản lý",
  "note": "Giám sát bởi Trạm BVTV huyện"
}
```

| Trường | Kiểu | Bắt buộc | Ràng buộc |
|---|---|---:|---|
| `reason` | string | Có | Không rỗng sau trim, tối đa 100 ký tự (đồng bộ `CancelProductionLotRequest`) |
| `handlingMeasure` | string | **Có (TC-03)** | Không rỗng sau trim, tối đa 1000 ký tự — biện pháp xử lý lô |
| `note` | string | Không | Tối đa 1000 ký tự — diễn giải thêm |

**Điều kiện nghiệp vụ (thứ tự thực thi, đồng bộ pattern `cancelProductionLot`):**

1. Lô tồn tại → nếu không: `400` "Không tìm thấy lô sản xuất".
2. Lô thuộc tổ chức hiện tại (QTN-01) → nếu không: `400` "Lô sản xuất không thuộc tổ chức của bạn".
3. Trạng thái lô:
   - Trạng thái cuối (`CANCELLED`, `CLOSED`, `RECALLED`, `DISPOSED`) → `400` "Lô đã ở trạng thái {STATUS}, không thể loại bỏ".
   - Trạng thái khác `HARVESTED`/`PREPROCESSED`/`PACKAGED` (`DRAFT`, `PENDING`, `APPROVED`, `REJECTED`) → `400` "Chỉ có thể loại bỏ lô ở trạng thái HARVESTED, PREPROCESSED hoặc PACKAGED".
4. Lô chưa có lô hàng/tem (không có shipment) → nếu không: `400` "Lô đã sinh mã truy xuất, không thể loại bỏ. Vui lòng sử dụng luồng thu hồi lô".

**Hiệu ứng (PROPOSED — Backend implement):**

- `status` → `DISPOSED` (giá trị mới của `ProductionLotStatus`, trạng thái cuối).
- Lưu `disposal_reason`, `handling_measure`, `disposal_note`, `disposed_by`, `disposed_at` (migration mới, đặt tên theo convention hiện hành `V{yyyyMMddHHmmss}__…`).
- Activity log: `action = "DISPOSE"`, `entityType = "ProductionLot"`, mô tả kèm lý do.

**Response `200 OK`** — shape `CreateProductionLotResponse` (đồng bộ `/cancel`) + trường mới (PROPOSED):

```json
{
  "success": true,
  "status": 200,
  "data": {
    "id": "3f0d7a1e-…",
    "name": "Lô rau thơm vụ thu",
    "status": "DISPOSED",
    "disposalReason": "Kết quả kiểm nghiệm không đạt dư lượng thuốc bảo vệ thực vật",
    "handlingMeasure": "Phá hủy toàn bộ 200 kg tại khu cách ly theo hướng dẫn của cơ quan quản lý",
    "disposalNote": "Giám sát bởi Trạm BVTV huyện",
    "disposedByName": "Trần Văn A",
    "disposedAt": "2026-09-05T10:30:00"
  }
}
```

**Lỗi validation (400 — TC-03):** trả theo format `MethodArgumentNotValidException` của `GlobalExceptionHandler`:

```json
{
  "success": false,
  "status": 400,
  "message": "Dữ liệu không hợp lệ",
  "errors": {
    "reason": "Lý do loại bỏ không được để trống",
    "handlingMeasure": "Biện pháp xử lý không được để trống"
  },
  "path": "/api/v1/production-lots/3f0d7a1e-…/dispose",
  "timestamp": "2026-09-05T03:10:00Z"
}
```

Thiếu chỉ một trong hai trường bắt buộc cũng bị chặn với đúng key lỗi tương ứng.

Sau khi `DISPOSED`: lô bị loại khỏi mọi luồng nghiệp vụ (tạo lô hàng, kích hoạt tem, tạo yêu cầu kiểm nghiệm, hủy lô); vẫn hiển thị trong lịch sử/truy xuất với nhãn "Đã loại bỏ".

### 5.4 [MODIFY] `POST /api/v1/production-lots/{id}/cancel` — chặn hủy lô chưa đạt

- **Thay đổi:** thêm điều kiện loại trừ **sau** kiểm tra "đã sinh mã truy xuất":
  - Nếu loại nông sản của lô `requiresInspection == true` và **yêu cầu kiểm nghiệm hoàn tất mới nhất** (status `PASSED`/`FAILED`) của lô có trạng thái `FAILED` → từ chối:
    - `409 CONFLICT` — "Lô sản xuất chưa đạt kiểm nghiệm, không thể hủy. Vui lòng loại bỏ lô hoặc tạo yêu cầu kiểm nghiệm lại."
  - Lô chưa kiểm nghiệm hoặc kết luận mới nhất là `PASSED` → hủy như hiện tại.
- **Lý do:** QTN-30 yêu cầu hệ thống **yêu cầu xử lý theo 1 trong 2 hướng** (loại bỏ / kiểm nghiệm lại); nếu không chặn, `/cancel` trở thành lối thoát thứ ba không ghi biện pháp xử lý (GAP-5).
- **Impact FE:** màn hình hủy lô phải hiện thông báo và dẫn về 2 lựa chọn trên. Contract còn lại của `/cancel` giữ nguyên (xem `farm/CancelProductionLot.md`).

### 5.5 [MODIFY] `POST /api/v1/production-lots/{lotId}/test-requests` — chặn lô đã loại bỏ

- **Thay đổi:** thêm `DISPOSED` vào danh sách trạng thái loại trừ hiện có (`REJECTED`, `CANCELLED`):
  - `400` — "Lô sản xuất đã bị loại bỏ, không thể tạo yêu cầu kiểm nghiệm."
- **Không thay đổi:** lô có yêu cầu `FAILED` **vẫn được tạo vòng kiểm nghiệm mới** (đây chính là luồng kiểm nghiệm lại, TC-02). Chỉ yêu cầu đang `PENDING_RESULT` mới tham gia kiểm tra trùng bộ chỉ tiêu (giữ nguyên hành vi `409` + `confirmDuplicate`).

### 5.6 [MODIFY] `DELETE /api/v1/inspection-results/{resultId}` — không xóa kết quả đã kết luận (TC-04)

- **Thay đổi:** chỉ cho phép xóa khi yêu cầu kiểm nghiệm chứa chỉ tiêu còn ở `PENDING_RESULT`:
  - Request đã `PASSED`/`FAILED`/`CANCELLED` → `409 CONFLICT` — "Không thể xóa kết quả của yêu cầu kiểm nghiệm đã có kết luận."
- **Lý do:** chặn việc xóa bằng chứng Không đạt của yêu cầu đã kết luận rồi ghi lại kết quả Đạt (TC-04: "Không có cơ chế cho phép ẩn/xóa kết quả Không đạt thông qua API nghiệp vụ").
- **Chỉnh sửa dữ liệu sai khi đã kết luận:** ghi/cập nhật lại kết quả qua `PUT /api/v1/inspection-requests/{requestId}/results` (đã được phép với request `FAILED`) — mọi thay đổi được lưu vết qua activity log; không xóa bản ghi.
- **Impact FE:** ẩn nút xóa kết quả khi request đã kết luận; bắt lỗi `409` khi bị chặn.

### 5.7 [REUSE] Kiểm nghiệm lại (TC-02)

Luồng kiểm nghiệm lại dùng lại nguyên trạng các API hiện có, **không cần endpoint mới**:

1. `POST /api/v1/production-lots/{lotId}/test-requests` — tạo **vòng kiểm nghiệm mới** (vòng N). Yêu cầu cũ `FAILED` không chặn việc tạo vòng mới.
2. `PUT /api/v1/inspection-requests/{requestId}/results` (hoặc `POST /api/v1/inspection-criteria/{criterionId}/results` từng chỉ tiêu) — ghi kết quả vòng mới.
3. Khi mọi chỉ tiêu Đạt và còn hiệu lực → request tự chốt `PASSED`. Lịch sử giữ lại cả vòng `FAILED` cũ lẫn vòng `PASSED` mới.
4. `POST /api/v1/production-lots/{lotId}/can-activate-seal` — xác nhận `canActivate = true` (logic "kết quả mới nhất theo mã chỉ tiêu trên toàn bộ yêu cầu" đã ưu tiên vòng mới).
5. `POST /api/v1/shipments` — tạo lô hàng bình thường (gate §5.1 mở).

> Đường dẫn phụ đã tồn tại: request `FAILED` cho phép ghi/cập nhật lại kết quả trên chính nó (xem `inspection-result.md` §2). Cả hai cách đều thỏa TC-02; khuyến nghị tạo vòng mới để lịch sử tách bạch từng lần kiểm nghiệm.

### 5.8 [REUSE] Lịch sử kiểm nghiệm và lịch sử xử lý (TC-04)

| API | Nội dung phục vụ TC-04 |
|---|---|
| `GET /api/v1/test-requests?lotId={id}` | Danh sách **tất cả** vòng kiểm nghiệm của lô, gồm cả `FAILED` và `PASSED`; mỗi item có `failedCriteriaCount`, `failedRatio` |
| `GET /api/v1/inspection-requests/{requestId}` | Chi tiết vòng kiểm nghiệm: snapshot chỉ tiêu + kết quả từng chỉ tiêu (`passed = false` hiển thị như đã ghi) |
| `GET /api/v1/inspection-requests/{requestId}/results` | Kết quả từng chỉ tiêu của một vòng |
| Tra cứu công khai (`publicapi`) | Kết quả Không đạt hiển thị trong hồ sơ truy xuất công khai (`failedCriteriaCount` > 0 không bị ẩn) |
| `GET /api/v1/organizations/activity-logs` | Lịch sử xử lý: tạo/ghi/xóa kết quả kiểm nghiệm, loại bỏ lô (`action = DISPOSE`), hủy lô |

Không bổ sung bất kỳ API ẩn/xóa kết quả Không đạt nào. Sau thay đổi §5.6, không còn đường nghiệp vụ nào xóa kết luận Không đạt của yêu cầu đã chốt.

---

## 6. Error contract tổng hợp

Project **không sử dụng business error code**; lỗi được phân biệt qua HTTP status, `message` (tiếng Việt) và `errors` (object/map khi có). Format chung theo `GlobalExceptionHandler` + `ApiResult`.

| # | Tình huống | API | HTTP | Message / errors |
|---|---|---|---|---|
| E-01 | Lô không tồn tại | dispose, cancel, create shipment | 400 | "Không tìm thấy lô sản xuất" |
| E-02 | Người dùng không có quyền (không phải VT-02) | mọi endpoint trong story | 403 | Handler `AccessDeniedException` — "Bạn không có quyền thực hiện chức năng này" |
| E-03 | Lô không thuộc tổ chức người dùng (QTN-01) | mọi endpoint trong story | 400 | "Bạn không thuộc tổ chức của lô sản xuất." / "Lô sản xuất không thuộc tổ chức của bạn" |
| E-04 | Lô chưa có kết quả kiểm nghiệm | create shipment, activate | 409 | `reasonCode = INSPECTION_MISSING` — "Lô sản xuất chưa có kết quả kiểm nghiệm đạt cho tất cả chỉ tiêu, không thể tạo lô hàng." |
| E-05 | Lô có kết quả Không đạt | create shipment, activate | 409 | `reasonCode = INSPECTION_FAILED` — "Lô sản xuất chưa đạt kiểm nghiệm, không thể tạo lô hàng." |
| E-06 | Lô đang chờ kiểm nghiệm / đang chờ kết quả kiểm nghiệm lại | create shipment, activate | 409 | `reasonCode = INSPECTION_PENDING` — "Lô sản xuất đang chờ kết quả kiểm nghiệm, không thể tạo lô hàng." |
| E-07 | Kết quả kiểm nghiệm hết hiệu lực | create shipment, activate | 409 | `reasonCode = INSPECTION_EXPIRED` — "Kết quả kiểm nghiệm đã hết hiệu lực, không thể tạo lô hàng." |
| E-08 | Lô đã bị loại bỏ | dispose (gọi lại), test-requests | 400 | dispose: "Lô đã ở trạng thái DISPOSED, không thể loại bỏ"; test-requests: "Lô sản xuất đã bị loại bỏ, không thể tạo yêu cầu kiểm nghiệm." (create shipment/activate bị chặn sẵn bởi điều kiện `PACKAGED` hiện có) |
| E-09 | Thiếu lý do loại bỏ | dispose | 400 | `errors.reason = "Lý do loại bỏ không được để trống"` |
| E-10 | Thiếu biện pháp xử lý | dispose | 400 | `errors.handlingMeasure = "Biện pháp xử lý không được để trống"` |
| E-11 | Yêu cầu kiểm nghiệm lại không hợp lệ (criteria rỗng/trùng, `sampleSentDate` tương lai, lô sai điều kiện) | test-requests | 400/409 | Giữ nguyên contract `inspection-request.md` §3 |
| E-12 | Cố tạo Lô hàng từ lô chưa đạt (TC-01) | create shipment | 409 | `reasonCode = INSPECTION_FAILED` — xem §5.1 |
| E-13 | Xóa kết quả của yêu cầu đã kết luận | delete result | 409 | "Không thể xóa kết quả của yêu cầu kiểm nghiệm đã có kết luận." |
| E-14 | Hủy lô chưa đạt | cancel | 409 | "Lô sản xuất chưa đạt kiểm nghiệm, không thể hủy. Vui lòng loại bỏ lô hoặc tạo yêu cầu kiểm nghiệm lại." |

---

## 7. Validation tổng hợp

| Endpoint | Ràng buộc đầu vào mới / thay đổi |
|---|---|
| `POST /api/v1/shipments`, `POST /api/v1/shipments/{id}/activate` | Không thêm ràng buộc payload; thêm điều kiện nghiệp vụ gate QTN-30 (§5.1) |
| `POST /api/v1/production-lots/{id}/dispose` | `reason` bắt buộc ≤100; `handlingMeasure` bắt buộc ≤1000; `note` tùy chọn ≤1000; trạng thái lô hợp lệ; chưa có shipment |
| `POST /api/v1/production-lots/{id}/cancel` | Giữ nguyên payload; thêm điều kiện loại trừ kết luận `FAILED` (§5.4) |
| `POST /api/v1/production-lots/{lotId}/test-requests` | Giữ nguyên payload; thêm loại trừ `DISPOSED` (§5.5) |
| `DELETE /api/v1/inspection-results/{resultId}` | Thêm điều kiện request còn `PENDING_RESULT` (§5.6) |

---

## 8. Tương thích ngược

| Thành phần phụ thuộc | API liên quan | Tác động | Đánh giá |
|---|---|---|---|
| NCL-04-CN-002 (tạo lô hàng + sinh mã) | `POST /api/v1/shipments` | Chỉ thêm 1 error case (409) cho lô bắt buộc kiểm nghiệm chưa đạt; payload + response thành công không đổi; lô thuộc loại không bắt buộc kiểm nghiệm không bị ảnh hưởng | Không phá vỡ |
| NCL-04-CN-002 FE (`frontend/src/api/shipmentApi.ts`) | `createShipment`, `activateShipmentStamps` | Phải xử lý 409 + `reasonCode` mới, hiển thị 2 lựa chọn xử lý | Cập nhật FE |
| QTN-21 (chặn kích hoạt tem) | `POST /api/v1/shipments/{id}/activate` | Bổ sung enforcement còn thiếu; đúng nghĩa QTN-21, không đổi contract thành công | Không phá vỡ |
| NCL-11-CN-003 (luồng sơ chế / sự kiện) | event APIs | Không thay đổi | Không ảnh hưởng |
| Chức năng kiểm nghiệm hiện tại | test-requests, results APIs | Không đổi payload/response; chỉ thêm loại trừ `DISPOSED` và giới hạn `DELETE` result | Không phá vỡ; thay đổi hành vi nhỏ ở DELETE (§5.6) |
| NCL-02-CN-006 (hủy lô) | `POST /api/v1/production-lots/{id}/cancel` | Thêm 1 error case (409) cho lô kết luận `FAILED` | Cập nhật FE màn hình hủy |
| Dữ liệu hiện hữu | Lô hàng đã tạo từ lô chưa đạt trước khi gate có hiệu lực | Sẽ không thể kích hoạt tem (409); cần policy thu hồi/xử lý — hỏi PO (§12) | Cần quyết định |

## 9. Điểm Backend cần implement

1. `ShipmentServiceImpl.createShipment` — thêm gate QTN-30 (§5.1). Tách logic "kết quả mới nhất theo chỉ tiêu của lô" thành service chung (ví dụ `InspectionEligibilityService`) để `checkCanActivateSeal` và gate dùng chung một nguồn sự thật.
2. `ShipmentServiceImpl.activateShipmentStamps` — enforce cùng gate (§5.2).
3. `ProductionLotController` + `ProductionLotService` — endpoint `POST /{id}/dispose` (§5.3): DTO `DisposeProductionLotRequest` (pattern `CancelProductionLotRequest`), lưu cột mới, activity log `action = "DISPOSE"`.
4. `ProductionLotStatus` — thêm `DISPOSED`; dashboard/báo cáo loại `DISPOSED` khỏi sản lượng đang canh tác như `CANCELLED` (bucket `byStatus` tự có do lặp `values()`).
5. Migration mới: thêm cột `disposal_reason`, `handling_measure`, `disposal_note`, `disposed_by`, `disposed_at` vào `production_lot` (`V{yyyyMMddHHmmss}__add_disposal_fields_to_production_lot.sql`).
6. `cancelProductionLot` — thêm chặn kết luận `FAILED` (§5.4).
7. `InspectionRequestServiceImpl.createInspectionRequest` — thêm loại trừ `DISPOSED` (§5.5).
8. `InspectionCriterionResultServiceImpl.deleteResult` — giới hạn `PENDING_RESULT` (§5.6).
9. Định nghĩa `reasonCode` + `errors` chi tiết cho gate (§5.1) qua `BusinessException(HttpStatus.CONFLICT, message, details)`.
10. Unit test: gate cả 4 tình trạng (`MISSING/PENDING/FAILED/EXPIRED`), dispose TC-03, cancel chặn lô `FAILED`, deleteResult chặn request kết luận, test-requests chặn `DISPOSED`.

## 10. Điểm Frontend cần sử dụng

1. `frontend/src/api/shipmentApi.ts` — xử lý 409 mới của `createShipment`/`activateShipmentStamps`: đọc `errors.reasonCode`, hiển thị message + gợi ý 2 hướng xử lý.
2. `frontend/src/api/productionLotApi.ts` — thêm `disposeProductionLot(id, body)` (NEW); màn hình chi tiết lô: nút "Loại bỏ lô" chỉ hiện cho lô chưa đạt và chưa có lô hàng; dialog bắt buộc `reason` + `handlingMeasure` (validate client-side) — TC-03.
3. Màn hình tạo lô hàng: trước khi mở form, gọi `POST /api/v1/production-lots/{lotId}/can-activate-seal`; nếu `canActivate = false` hiển thị trạng thái kiểm nghiệm của lô (thống kê chỉ tiêu) và khóa nút tiếp tục.
4. Màn hình yêu cầu kiểm nghiệm — nút "Tạo yêu cầu kiểm nghiệm lại" cho lô có vòng `FAILED`; hiển thị nhãn vòng (lần 1, lần 2…) từ `GET /test-requests?lotId=`; timeline giữ cả vòng `FAILED` lẫn `PASSED`.
5. Màn hình hủy lô — bắt lỗi 409 (E-14), hướng dẫn sang "Loại bỏ lô" hoặc "Kiểm nghiệm lại".
6. Trang chi tiết kết quả kiểm nghiệm — ẩn nút xóa kết quả khi request đã `PASSED`/`FAILED`/`CANCELLED` (§5.6).

---

## 11. Đối chiếu acceptance criteria

| TC | Đáp ứng bởi |
|---|---|
| TC-01 | Gate §5.1 tại `POST /api/v1/shipments` (409 + `INSPECTION_FAILED`); không tạo shipment, không sinh mã/tem, không trừ hạn mức |
| TC-02 | §5.7: tạo vòng mới qua API REUSE; vòng `FAILED` giữ trong lịch sử; sau khi vòng mới `PASSED`, gate mở và tạo lô hàng theo điều kiện hiện có |
| TC-03 | §5.3 dispose: `reason` + `handlingMeasure` bắt buộc, thiếu bất kỳ trường nào → 400 kèm map lỗi trường |
| TC-04 | §5.8 (lịch sử REUSE trả cả `FAILED` và `PASSED`) + §5.6 (không xóa kết quả của yêu cầu đã kết luận) + public lookup không ẩn kết quả Không đạt |

## 12. Điểm chưa đủ thông tin

| # | Điểm chưa rõ | Xử lý đề xuất |
|---|---|---|
| 1 | HTTP status cho gate: đề xuất `409`, nhưng các lỗi trạng thái hiện có của create shipment đang trả `400` (doc cũ ghi `409` — GAP-6) | Backend Agent chốt: chỉ gate mới dùng 409 (an toàn, không phá FE hiện có) hay đồng bộ toàn bộ lên 409 |
| 2 | Policy cho dữ liệu lọt (lô hàng đã tạo từ lô chưa đạt trước khi gate có hiệu lực) | Đề xuất dùng luồng thu hồi (`trace/ShipmentRecall.md`); cần PO quyết định |
| 3 | Dispose có dành cho lô kết luận Đạt còn hiệu lực không? Thiết kế hiện tại cho phép nếu lô chưa có shipment | Đề xuất giữ cho phép; PO xác nhận |
| 4 | `reason` của dispose: free-text hay danh sách cố định như cancel (Mất mùa/Sâu bệnh/Khai báo nhầm/Lý do khác)? | Đề xuất free-text ≤100 + gợi ý UI; PO xác nhận |
| 5 | Có cần bổ sung `reasonCode` (machine-readable) vào response `can-activate-seal` đồng bộ với gate không? | Đề xuất có (MODIFY nhỏ, không phá shape hiện có) |

## 13. Quyết định thiết kế

| # | Quyết định | Lý do |
|---|---|---|
| D-1 | Chặn tại `POST /shipments` (chính) và `POST /shipments/{id}/activate` (phụ) | QTN-30: chặn ngay tại API tạo Lô hàng, không giải quyết bằng cách chỉ sửa API kích hoạt tem |
| D-2 | Gate trả `409 CONFLICT` + `errors.reasonCode` | Phân biệt máy được lỗi trạng thái nghiệp vụ với `400` validation; `BusinessException` đã hỗ trợ status + details, additive với FE |
| D-3 | "Không đạt kiểm nghiệm" là trạng thái suy diễn từ dữ liệu kiểm nghiệm; không thêm enum trạng thái cho nó | Tránh phá vỡ filter/dashboard/báo cáo đang liệt kê `ProductionLotStatus`; trạng thái đã thể hiện qua `test-requests` |
| D-4 | `DISPOSED` là trạng thái cuối, tách khỏi `CANCELLED` | Dispose là xử lý sau kết luận Không đạt và bắt buộc ghi biện pháp xử lý; cancel là từ bỏ hồ sơ trước khi có lô hàng |
| D-5 | Chặn cancel khi kết luận mới nhất là `FAILED` | QTN-30: hệ thống phải yêu cầu xử lý theo 1 trong 2 hướng |
| D-6 | Kiểm nghiệm lại = tạo vòng mới bằng `POST /test-requests` (REUSE), không tạo endpoint riêng | Implementation hiện tại đã hỗ trợ; tránh trùng lặp |
| D-7 | Giới hạn `DELETE result` cho request `PENDING_RESULT` | TC-04; chỉnh sửa dữ liệu đi qua `PUT results` có lưu vết |
| D-8 | Gate tính trên "kết quả mới nhất theo mã chỉ tiêu trên toàn bộ yêu cầu của lô" | Cùng nguồn sự thật với `checkCanActivateSeal`; vòng kiểm nghiệm lại `PASSED` tự động thay thế vòng `FAILED` |

---

## Nguồn code đối chiếu

- `backend/src/main/java/vn/nguongocso/trace/controller/ShipmentController.java`
- `backend/src/main/java/vn/nguongocso/trace/service/impl/ShipmentServiceImpl.java`
- `backend/src/main/java/vn/nguongocso/certification/controller/InspectionRequestController.java`
- `backend/src/main/java/vn/nguongocso/certification/controller/InspectionCriterionResultController.java`
- `backend/src/main/java/vn/nguongocso/certification/service/impl/InspectionCriterionResultServiceImpl.java`
- `backend/src/main/java/vn/nguongocso/farm/controller/ProductionLotController.java`
- `backend/src/main/java/vn/nguongocso/farm/service/impl/ProductionLotServiceImpl.java`
- `backend/src/main/java/vn/nguongocso/farm/entity/ProductionLot.java`
- `backend/src/main/java/vn/nguongocso/farm/enums/ProductionLotStatus.java`
- `backend/src/main/java/vn/nguongocso/farm/dto/request/CancelProductionLotRequest.java`
- `backend/src/main/java/vn/nguongocso/certification/enums/InspectionRequestStatus.java`
- `backend/src/main/java/vn/nguongocso/certification/dto/response/CanActivateSealCheckResponse.java`
- `backend/src/main/java/vn/nguongocso/exception/GlobalExceptionHandler.java`, `BusinessException.java`
- `backend/src/main/java/vn/nguongocso/common/ApiResult.java`
- Tài liệu: `docs/api/certification/inspection-request.md`, `inspection-result.md`, `inspection-criteria.md`; `docs/api/trace/CreateShipmentTraceCode.md`; `docs/api/farm/CancelProductionLot.md`; `docs/api/organization/ActivityHistory.md`







