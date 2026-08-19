# API Docs — Phát hiện sự kiện sai lô & Quản lý bản nháp

*Mã User Story: NCL-08-CN-002 Phát hiện sự kiện sai lô*

---

## 1. Thông tin chung

### 1.1 Mục tiêu
Hệ thống cần cung cấp các giải pháp nhằm phát hiện sớm và chặn đứng việc ghi nhận sự kiện chuỗi cung ứng vào các lô sản xuất (Production Lot) hoặc lô hàng (Shipment) không hợp lệ (không tồn tại, sai tổ chức, sai trạng thái, hoặc đã bị thu hồi). Qua đó ngăn ngừa việc làm hỏng dòng dữ liệu truy xuất nguồn gốc sản phẩm.

Các tính năng chính bao gồm:
1. **Kiểm tra trực tuyến (Validate Lot/Shipment)**: Hỗ trợ kiểm tra tính hợp lệ của lô trước khi người dùng thực hiện tạo sự kiện.
2. **Chặn ghi nhận & Lưu nhật ký lỗi (Failed Event Logger)**: Chặn ghi nhận sự kiện ở backend khi phát hiện lô không hợp lệ, đồng thời tự động lưu vết sự cố này vào nhật ký hệ thống (`failed_event_logs`) để làm cơ sở cảnh báo và khắc phục.
3. **Hủy bản nháp (Cancel/Delete Draft)**: Cho phép Người ghi sự kiện hủy các bản nháp sự kiện hoặc lô hàng nháp được gắn sai thông tin lô sản xuất, tạo điều kiện chỉnh sửa và chọn lại lô đúng.

### 1.2 Phân quyền và Vai trò
* **VT-02 (Quản lý hợp tác xã)**: Có quyền kiểm tra lô, hủy bản nháp, xem danh sách nhật ký lỗi.
* **VT-03 (Người ghi sự kiện)**: Có quyền kiểm tra lô, hủy bản nháp của mình, xem danh sách nhật ký lỗi.
* **VT-04 (Doanh nghiệp thu mua)**: Chỉ có quyền kiểm tra tính hợp lệ của lô hàng trước khi ghi sự kiện thu mua (`PROCUREMENT`).

---

## 2. API 1: Kiểm tra tính hợp lệ của Lô/Lô hàng trước khi tạo sự kiện

API này hỗ trợ giao diện Frontend truy vấn nhanh xem một Lô sản xuất hoặc Lô hàng có đủ điều kiện để ghi nhận một loại sự kiện cụ thể nào đó hay không.

### 2.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `GET` |
| **Endpoint** | `/api/v1/chain-events/validate-lot` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện), `VT-04` (Doanh nghiệp thu mua) |

### 2.2 Request Parameters

| Tham số | Kiểu dữ liệu | Bắt buộc | Mô tả / Ràng buộc |
| --- | --- | --- | --- |
| `lotId` | UUID | ✓ | ID của Lô sản xuất (`ProductionLot`) hoặc Lô hàng (`Shipment`). |
| `eventType` | String | ✓ | Loại sự kiện dự kiến ghi nhận. <br>Giá trị hợp lệ: `HARVEST`, `PACKAGING`, `TRANSPORT`, `PROCUREMENT`. |

### 2.3 Response Thành công (`200 OK` - Lô Hợp lệ)

**DTO:** `ApiResult<LotValidationResponse>`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "lotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
    "eventType": "PACKAGING",
    "valid": true,
    "message": "Lô sản xuất hợp lệ để ghi nhận sự kiện đóng gói.",
    "details": {
      "lotType": "PRODUCTION_LOT",
      "currentStatus": "HARVESTED",
      "organizationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    }
  },
  "timestamp": "2026-07-29T11:25:00Z"
}
```

### 2.4 Response Thành công (`200 OK` - Lô Không hợp lệ)

Trường hợp lô tồn tại nhưng trạng thái hoặc thông tin không hợp lệ cho sự kiện, API vẫn trả về `200 OK` nhưng trường `valid` sẽ là `false` kèm theo lý do cụ thể.

```json
{
  "success": true,
  "status": 200,
  "data": {
    "lotId": "a4d87b1c-c3b8-4c1f-bcb0-2b86737d1406",
    "eventType": "TRANSPORT",
    "valid": false,
    "message": "Lô hàng đã bị thu hồi, không thể ghi sự kiện vận chuyển.",
    "details": {
      "lotType": "SHIPMENT",
      "currentStatus": "RECALLED",
      "organizationId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    }
  },
  "timestamp": "2026-07-29T11:26:10Z"
}
```

### 2.5 Response Lỗi (`404 Not Found` - Không tìm thấy Lô)

Nếu không tồn tại thực thể lô nào khớp với `lotId` được cung cấp.

```json
{
  "success": false,
  "status": 404,
  "message": "Không tìm thấy thông tin lô sản xuất hoặc lô hàng với ID đã cho.",
  "path": "/api/v1/chain-events/validate-lot",
  "timestamp": "2026-07-29T11:27:00Z"
}
```

---

## 3. API 2: Hủy bỏ bản nháp sự kiện / lô hàng sai lô

Cho phép Người ghi sự kiện hoặc Quản lý hủy bản nháp (ví dụ: Lô hàng ở trạng thái `DRAFT` hoặc sự kiện nháp) khi phát hiện đã gán sai lô hàng. Hệ thống sẽ xóa hoặc đưa bản nháp về trạng thái hủy, giải phóng mã tem để người dùng có thể thực hiện chọn lại.

### 3.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `DELETE` |
| **Endpoint** | `/api/v1/chain-events/drafts/{id}` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 3.2 Path Parameter

* `id` (UUID - Bắt buộc): ID của bản ghi sự kiện/lô hàng đang ở trạng thái nháp (`DRAFT`).

### 3.3 Response Thành công (`200 OK`)

**DTO:** `ApiResult<Void>`

```json
{
  "success": true,
  "status": 200,
  "message": "Hủy bản nháp thành công, bạn có thể thực hiện chọn lại lô hàng.",
  "timestamp": "2026-07-29T11:30:00Z"
}
```

### 3.4 Response Lỗi (`409 Conflict`)

Khi bản nháp đã được kích hoạt hoặc đã chuyển sang trạng thái chính thức (không còn là `DRAFT`), hệ thống sẽ chặn không cho hủy.

```json
{
  "success": false,
  "status": 409,
  "message": "Không thể hủy bản nháp vì lô hàng đã được in mã hoặc kích hoạt tem.",
  "path": "/api/v1/chain-events/drafts/e305bb65-cfda-46df-9d58-45fa97f64245",
  "timestamp": "2026-07-29T11:31:00Z"
}
```

---

## 4. API 3: Truy vấn nhật ký các lần ghi sự kiện bị chặn (Sai lô)

Phục vụ công tác giám sát chất lượng dữ liệu và theo dõi hành vi nhập liệu sai của người dùng.

### 4.1 Thông tin API

| Thuộc tính | Giá trị |
| --- | --- |
| **Method** | `GET` |
| **Endpoint** | `/api/v1/chain-events/failed-logs` |
| **Authentication** | Bearer Token |
| **Quyền truy cập** | `VT-02` (Quản lý HTX), `VT-03` (Người ghi sự kiện) |

### 4.2 Request Parameters (Phân trang)

| Tham số | Kiểu dữ liệu | Bắt buộc | Mô tả |
| --- | --- | --- | --- |
| `page` | Integer | | Số trang cần lấy (bắt đầu từ `0`, mặc định là `0`). |
| `size` | Integer | | Số phần tử trên một trang (mặc định là `10`). |

### 4.3 Response Thành công (`200 OK`)

**DTO:** `ApiResult<PageResponse<FailedEventLogResponse>>`

```json
{
  "success": true,
  "status": 200,
  "data": {
    "items": [
      {
        "id": "c1f77ba3-fa34-4545-bb79-a78bcf04d1ef",
        "userId": "e458e0a8-b648-4cb2-9d32-dcf7429beee7",
        "userFullName": "Nguyễn Văn Ghi",
        "eventType": "TRANSPORT",
        "lotId": "85d91b0c-c3b8-4c1f-bcb0-2b86737d1406",
        "lotCode": "LH-20260729-001",
        "failureReason": "Lô hàng đã bị thu hồi, không thể ghi sự kiện vận chuyển.",
        "attemptedAt": "2026-07-29T18:24:12"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true
  },
  "timestamp": "2026-07-29T11:40:00Z"
}
```

---

## 5. Quy tắc nghiệp vụ (Business Rules)

### 5.1 Ràng buộc tính hợp lệ của Lô đối với từng Sự kiện (QTN-05)

Hệ thống sẽ từ chối ghi nhận sự kiện và cảnh báo khi lô không đáp ứng các tiêu chuẩn sau:

1. **Sự kiện Thu hoạch (`HARVEST`)**:
   * Đối tượng liên kết: `ProductionLot` (Lô sản xuất).
   * Lô sản xuất phải tồn tại và có trạng thái là `APPROVED`.
   * Người thực hiện phải thuộc tổ chức quản lý của lô sản xuất đó.
   * Lỗi nếu vi phạm: *"Lô sản xuất chưa được duyệt hoặc trạng thái không hợp lệ để thu hoạch."*
2. **Sự kiện Đóng gói (`PACKAGING`)**:
   * Đối tượng liên kết: `ProductionLot` (Lô sản xuất).
   * Lô sản xuất phải tồn tại và có trạng thái là `HARVESTED`.
   * Người thực hiện phải thuộc tổ chức quản lý của lô sản xuất đó.
   * Lỗi nếu vi phạm: *"Chỉ được ghi nhận sự kiện đóng gói cho lô đã thu hoạch."*
3. **Sự kiện Vận chuyển (`TRANSPORT`)**:
   * Đối tượng liên kết: `Shipment` (Lô hàng).
   * Lô hàng phải tồn tại và đã kích hoạt tem (`status == ACTIVATED`).
   * Lô hàng không được ở trạng thái đã thu hồi (`status == RECALLED`).
   * Người thực hiện phải thuộc tổ chức quản lý của lô hàng đó.
   * Lỗi nếu vi phạm: *"Lô hàng chưa được kích hoạt hoặc đã bị thu hồi."*
4. **Sự kiện Thu mua (`PROCUREMENT`)**:
   * Đối tượng liên kết: `Shipment` (Lô hàng).
   * Lô hàng phải tồn tại và có trạng thái đã kích hoạt (`status == ACTIVATED`).
   * Lô hàng không được ở trạng thái đã thu hồi (`status == RECALLED`).
   * Lỗi nếu vi phạm: *"Lô hàng không hoạt động hoặc đã bị thu hồi."*

### 5.2 Cơ chế Lưu Lịch sử ghi nhận Thất bại (Failed Event Logging)

Khi có bất cứ yêu cầu ghi sự kiện nào (`POST /api/v1/chain-events/*`) bị chặn do vi phạm quy tắc trạng thái lô ở mục 5.1:
1. Hệ thống **không** lưu sự kiện vào bảng `chain_events`.
2. Hệ thống thực hiện lưu một bản ghi mới vào bảng `failed_event_logs` chứa:
   * ID người dùng thực hiện (`userId`).
   * Loại sự kiện cố gắng ghi (`eventType`).
   * ID của lô liên quan (`lotId`).
   * Lý do lỗi chi tiết (`failureReason`).
   * Thời gian cố gắng ghi nhận (`attemptedAt`).
3. Trả về mã lỗi thích hợp (thường là `400 Bad Request` hoặc `409 Conflict`) kèm cảnh báo sai lô cho người dùng.

### 5.3 Quy tắc hủy bản nháp (Draft Deletion)
* Chỉ hỗ trợ hủy bỏ với các lô hàng/sự kiện đang có trạng thái nháp `DRAFT`.
* Nếu là lô hàng nháp (`Shipment` ở trạng thái `DRAFT` hoặc `CODE_PRINTED` nhưng chưa kích hoạt `ACTIVATED`), khi hủy:
  * Thu hồi/Giải phóng dải mã số thứ tự trong `CodeRange`. Giảm `usedCount` của dải mã tương ứng để tránh hao hụt tài nguyên tem.
  * Xóa các mã tem `TraceCode` đã được tạo tạm thời liên kết với lô hàng đó.
  * Xóa thực thể `Shipment` nháp khỏi cơ sở dữ liệu.

---

## 6. Luồng xử lý chi tiết phía Backend

### 6.1 Luồng ghi sự kiện có kiểm tra trạng thái lô & lưu nhật ký lỗi

```
Client                      ChainEventService          FailedEventLogRepository       Database
  │                                 │                             │                      │
  │─── POST /chain-events/transport ──>                           │                      │
  │    (ShipmentId)                 │                             │                      │
  │                                 │─── Truy vấn Shipment ─────────────────────────────>│
  │                                 │<── Trả về Shipment (RECALLED) ─────────────────────│
  │                                 │                             │                      │
  │                                 │─── [Phát hiện Lô sai/Thu hồi]                      │
  │                                 │                             │                      │
  │                                 │─── Tạo và lưu FailedEventLog ─────────────────────>│
  │                                 │    (Reason: Lô hàng đã bị thu hồi)                 │
  │                                 │<── Xác nhận lưu thành công ────────────────────────│
  │                                 │                             │                      │
  │<── Response 409 Conflict ───────│                             │                      │
  │    (Lô hàng đã bị thu hồi)      │                             │                      │
```

### 6.2 Luồng hủy bản nháp lô hàng gắn sai lô sản xuất

```
Client                      ShipmentService             TraceCodeRepository           Database
  │                                 │                             │                      │
  │─── DELETE /drafts/{shipmentId} ──>                            │                      │
  │                                 │                             │                      │
  │                                 │─── Truy vấn Shipment ─────────────────────────────>│
  │                                 │<── Trả về Shipment (DRAFT) ────────────────────────│
  │                                 │                             │                      │
  │                                 │─── Xóa tất cả TraceCode liên quan ────────────────>│
  │                                 │<── Xác nhận xóa thành công ────────────────────────│
  │                                 │                             │                      │
  │                                 │─── Hoàn lại số lượng đã dùng cho CodeRange ───────>│
  │                                 │─── Xóa Shipment nháp ──────────────────────────────>│
  │                                 │<── Xác nhận xóa thành công ────────────────────────│
  │                                 │                             │                      │
  │<── Response 200 OK ─────────────│                             │                      │
```
